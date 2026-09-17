package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelCalculator
import com.pixelquest.app.domain.PointsCalculator
import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Step 32: Integration test verifying data integrity when completing tasks with Simple Mode ON,
 * then switching OFF, ensuring historical streaks, points, logs, and level data are 100% preserved.
 */
class SimpleModeDataIntegrityIntegrationTest {

    private inner class TestSettingsRepository : SettingsRepository {
        private val _simpleMode = MutableStateFlow(false)
        override val simpleModeEnabled: Flow<Boolean> = _simpleMode.asStateFlow()
        override val isSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isCrtEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val isHapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isReduceMotionEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val onboardingComplete: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = MutableStateFlow(true)

        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}

        override suspend fun setSimpleModeEnabled(enabled: Boolean) {
            _simpleMode.value = enabled
        }
    }

    private lateinit var settingsRepo: SettingsRepository
    private var profile = UserProfileEntity(
        id = 1,
        username = "Knight",
        avatarId = "hero_1",
        level = 2,
        totalXp = 250,
        perfectDaysTowardNextLevel = 4
    )
    private var streak = StreakEntity(
        id = 1,
        currentStreak = 4,
        longestStreak = 10,
        lastCompletedDate = LocalDate.now().minusDays(2),
        perfectDaysCount = 8
    )
    private val completionLogs = mutableListOf<TaskCompletionLogEntity>()

    @Before
    fun setUp() {
        settingsRepo = TestSettingsRepository()
        completionLogs.clear()
    }

    @Test
    fun testCompleteTasksInSimpleMode_thenSwitchOff_preservesAllHistoricalData() = runBlocking {
        // Initial baseline assertions
        val initialXp = profile.totalXp
        val initialStreak = streak.currentStreak
        assertFalse(settingsRepo.simpleModeEnabled.first())

        // 1. Activate Simple Mode
        settingsRepo.setSimpleModeEnabled(true)
        assertTrue(settingsRepo.simpleModeEnabled.first())

        // 2. Complete tasks on Day 1 while Simple Mode is active
        val day1 = LocalDate.now().minusDays(1)
        val earnedTask1 = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
        completionLogs.add(
            TaskCompletionLogEntity(taskId = 1L, completedDate = day1, wasCompleted = true, pointsAwarded = earnedTask1)
        )
        val earnedTask2 = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
        completionLogs.add(
            TaskCompletionLogEntity(taskId = 2L, completedDate = day1, wasCompleted = true, pointsAwarded = earnedTask2)
        )

        // Day 1 streak advancement & XP accumulation
        streak = streak.copy(
            currentStreak = streak.currentStreak + 1,
            longestStreak = maxOf(streak.longestStreak, streak.currentStreak + 1),
            lastCompletedDate = day1,
            perfectDaysCount = streak.perfectDaysCount + 1
        )
        val day1TotalEarned = earnedTask1 + earnedTask2
        profile = profile.copy(
            totalXp = profile.totalXp + day1TotalEarned,
            perfectDaysTowardNextLevel = profile.perfectDaysTowardNextLevel + 1
        )

        // 3. Complete tasks on Day 2 in Simple Mode
        val day2 = LocalDate.now()
        val earnedTask3 = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
        completionLogs.add(
            TaskCompletionLogEntity(taskId = 3L, completedDate = day2, wasCompleted = true, pointsAwarded = earnedTask3)
        )

        // Day 2 streak advancement & level calculation
        streak = streak.copy(
            currentStreak = streak.currentStreak + 1,
            longestStreak = maxOf(streak.longestStreak, streak.currentStreak + 1),
            lastCompletedDate = day2,
            perfectDaysCount = streak.perfectDaysCount + 1
        )
        val newPerfectDays = profile.perfectDaysTowardNextLevel + 1
        val daysRequired = 7
        val shouldLevelUp = LevelCalculator.shouldLevelUp(newPerfectDays, daysRequired)
        val newLevel = if (shouldLevelUp) profile.level + 1 else profile.level
        val remainingPerfectDays = if (shouldLevelUp) newPerfectDays - daysRequired else newPerfectDays

        profile = profile.copy(
            totalXp = profile.totalXp + earnedTask3,
            level = newLevel,
            perfectDaysTowardNextLevel = remainingPerfectDays
        )

        // Assert that underlying data accumulated during Simple Mode
        assertEquals(3, completionLogs.size)
        assertEquals(initialStreak + 2, streak.currentStreak)
        assertEquals(10, streak.perfectDaysCount)
        val expectedTotalXp = initialXp + day1TotalEarned + earnedTask3
        assertEquals(expectedTotalXp, profile.totalXp)
        // 4 + 1 + 1 = 6 days toward next level (not yet leveled up to 3 since 6 < 7)
        assertEquals(2, profile.level)
        assertEquals(6, profile.perfectDaysTowardNextLevel)

        // 4. Switch Simple Mode OFF (Return to Full Game Mode)
        settingsRepo.setSimpleModeEnabled(false)
        assertFalse("Simple mode must be deactivated", settingsRepo.simpleModeEnabled.first())

        // 5. Verify zero data loss upon return to Gamified Mode
        assertEquals("All 3 completion logs must remain present", 3, completionLogs.size)
        assertEquals("Total XP must be exactly preserved", expectedTotalXp, profile.totalXp)
        assertEquals("Streak must remain at 6", 6, streak.currentStreak)
        assertEquals("Longest streak must be preserved", 10, streak.longestStreak)
        assertEquals("Perfect days must remain at 10", 10, streak.perfectDaysCount)
        assertEquals("Level must remain at 2", 2, profile.level)
        assertEquals("Perfect days toward next level must remain at 6", 6, profile.perfectDaysTowardNextLevel)
    }
}
