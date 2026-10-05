package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelCalculator
import com.pixelquest.app.domain.PointsCalculator
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.CrtFilterPolicy
import com.pixelquest.app.notification.NotificationContentBuilder
import com.pixelquest.app.notification.ReminderContext
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Step 33: Integration test verifying zero behavior change in normal gamified operation
 * when Simple Mode is OFF (regression baseline).
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
class GamifiedModeBaselineRegressionTest {

    private val levelUps = LevelUpSignalManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())

    private var profile = UserProfileEntity(
        id = 1,
        username = "BaselineHero",
        avatarId = "hero_1",
        level = 1,
        totalXp = 100,
        perfectDaysTowardNextLevel = 6
    )
    private var streak = StreakEntity(
        id = 1,
        currentStreak = 3,
        longestStreak = 7,
        lastCompletedDate = LocalDate.now().minusDays(1),
        perfectDaysCount = 6
    )
    private val isSimpleMode = false

    @Before
    fun setUp() {
        levelUps.clearPendingLevelUp()
    }

    @Test
    fun testGamifiedMode_operatesWithFullFlourishAndZeroRegression() = runBlocking {
        // 1. Assert Simple Mode is OFF
        assertFalse(isSimpleMode)

        // 2. Complete task and earn XP with streak multiplier
        val taskXp = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
        val completionLog = TaskCompletionLogEntity(
            taskId = 42L,
            completedDate = LocalDate.now(),
            wasCompleted = true,
            pointsAwarded = taskXp
        )
        profile = profile.copy(totalXp = profile.totalXp + taskXp)
        assertEquals(100 + taskXp, profile.totalXp)
        assertTrue("Task points must be awarded in gamified baseline", completionLog.pointsAwarded > 0)

        // 3. Increment streak and perfect days
        val newPerfectDays = profile.perfectDaysTowardNextLevel + 1
        val daysRequired = 7
        val shouldLevelUp = LevelCalculator.shouldLevelUp(newPerfectDays, daysRequired)
        assertTrue("7 perfect days must trigger level up", shouldLevelUp)

        val newLevel = profile.level + 1
        profile = profile.copy(level = newLevel, perfectDaysTowardNextLevel = 0)
        assertEquals(2, profile.level)

        // 4. Level-up celebration signal emission under Gamified Mode
        levelUps.setPendingLevelUp(newLevel, suppressCelebration = isSimpleMode)
        val pendingCelebration = levelUps.pendingLevelUp.value
        assertNotNull("Gamified mode must emit level-up celebration signal", pendingCelebration)
        assertEquals(2, pendingCelebration)

        // 5. CRT scanline policy in Gamified Mode (Pixel theme + user enabled)
        val crtActive = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Pixel,
            isSimpleModeEnabled = isSimpleMode
        )
        assertTrue("CRT scanlines must be permitted in Gamified Mode when user enabled", crtActive)

        // 6. Notification copy contains gamified streak urgency
        val reminder = NotificationContentBuilder.reminder(
            ReminderContext(taskName = "Meditate", isSimpleMode = isSimpleMode, currentStreak = streak.currentStreak, doneToday = 0, totalToday = 1)
        )
        assertTrue("Gamified reminder must include streak motivation", reminder.bigText.contains("streak", ignoreCase = true))

        // Since Day 26 a missed quest's notice no longer claims the streak broke (one miss may not).
        val missedText = NotificationHelper.getMissedTaskText("Meditate", isSimpleMode)
        assertTrue("Gamified missed notice keeps its quest tone", missedText.contains("slipped past its time"))
        assertFalse(missedText.contains("streak", ignoreCase = true))
    }
}
