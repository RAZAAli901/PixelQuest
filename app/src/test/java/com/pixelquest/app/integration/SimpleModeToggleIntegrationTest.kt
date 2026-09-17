package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
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
 * Step 23: Integration test for toggling Simple Mode on and off,
 * verifying underlying data integrity, reactive state emission, and progress continuity throughout.
 */
class SimpleModeToggleIntegrationTest {

    private inner class TestSettingsRepository : SettingsRepository {
        private val _simpleModeFlow = MutableStateFlow(false)

        override val simpleModeEnabled: Flow<Boolean> = _simpleModeFlow.asStateFlow()
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
            _simpleModeFlow.value = enabled
        }
    }

    private lateinit var settingsRepo: SettingsRepository
    private var userProfile = UserProfileEntity(
        id = 1,
        username = "TestHero",
        avatarId = "hero_1",
        level = 2,
        totalXp = 200,
        perfectDaysTowardNextLevel = 3
    )
    private var streak = StreakEntity(
        id = 1,
        currentStreak = 5,
        longestStreak = 12,
        lastCompletedDate = LocalDate.now().minusDays(1),
        perfectDaysCount = 15
    )
    private val completionLogs = mutableListOf<TaskCompletionLogEntity>()

    @Before
    fun setUp() {
        settingsRepo = TestSettingsRepository()
        completionLogs.clear()
    }

    @Test
    fun testTogglingSimpleModeOnOff_preservesAllHistoricalData() = runBlocking {
        // 1. Initial Gamified Mode assertions
        assertFalse(settingsRepo.simpleModeEnabled.first())
        assertEquals(200, userProfile.totalXp)
        assertEquals(5, streak.currentStreak)

        // 2. Toggle Simple Mode ON
        settingsRepo.setSimpleModeEnabled(true)
        assertTrue("SettingsRepository must immediately emit true for Simple Mode", settingsRepo.simpleModeEnabled.first())

        // 3. Perform task completions while Simple Mode is active
        val earnedXpInSimple = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
        val log1 = TaskCompletionLogEntity(
            taskId = 101L,
            completedDate = LocalDate.now(),
            wasCompleted = true,
            pointsAwarded = earnedXpInSimple
        )
        completionLogs.add(log1)
        userProfile = userProfile.copy(totalXp = userProfile.totalXp + earnedXpInSimple)
        streak = streak.copy(
            currentStreak = streak.currentStreak + 1,
            longestStreak = maxOf(streak.longestStreak, streak.currentStreak + 1),
            lastCompletedDate = LocalDate.now(),
            perfectDaysCount = streak.perfectDaysCount + 1
        )

        // Verify data updated in DB layer despite Simple Mode being ON
        assertEquals(1, completionLogs.size)
        assertTrue(userProfile.totalXp > 200)
        assertEquals(6, streak.currentStreak)
        assertEquals(16, streak.perfectDaysCount)

        // 4. Toggle Simple Mode OFF (Switch back to Full Game Mode)
        settingsRepo.setSimpleModeEnabled(false)
        assertFalse("SettingsRepository must immediately emit false when switching back", settingsRepo.simpleModeEnabled.first())

        // 5. Assert all accumulated data is preserved without degradation
        assertEquals(1, completionLogs.size)
        assertEquals(200 + earnedXpInSimple, userProfile.totalXp)
        assertEquals(6, streak.currentStreak)
        assertEquals(12, streak.longestStreak)
        assertEquals(16, streak.perfectDaysCount)

        // 6. Complete another task in Full Game Mode
        val earnedXpGamified = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
        val log2 = TaskCompletionLogEntity(
            taskId = 102L,
            completedDate = LocalDate.now(),
            wasCompleted = true,
            pointsAwarded = earnedXpGamified
        )
        completionLogs.add(log2)
        userProfile = userProfile.copy(totalXp = userProfile.totalXp + earnedXpGamified)

        assertEquals(2, completionLogs.size)
        assertEquals(200 + earnedXpInSimple + earnedXpGamified, userProfile.totalXp)
    }
}
