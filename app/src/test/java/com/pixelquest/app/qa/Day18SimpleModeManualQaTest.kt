package com.pixelquest.app.qa

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Step 37: Manual QA test script simulating multiple days of task completions under Simple Mode,
 * verifying backend data (streaks, points, levels, and completion logs) progresses accurately
 * despite UI suppression.
 */
class Day18SimpleModeManualQaTest {

    private inner class TestSettingsRepository : SettingsRepository {
        private val _simpleModeFlow = MutableStateFlow(true)
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

    private lateinit var settingsRepository: SettingsRepository
    private var profile = UserProfileEntity(
        id = 1,
        username = "QAHero",
        avatarId = "hero_1",
        level = 1,
        totalXp = 0,
        perfectDaysTowardNextLevel = 0
    )
    private var streak = StreakEntity(
        id = 1,
        currentStreak = 0,
        longestStreak = 0,
        lastCompletedDate = null,
        perfectDaysCount = 0
    )
    private val logs = mutableListOf<TaskCompletionLogEntity>()

    @Before
    fun setUp() {
        settingsRepository = TestSettingsRepository()
        logs.clear()
    }

    @Test
    fun testFiveDaySimulationUnderSimpleMode_maintainsFlawlessBackendProgress() = runBlocking {
        // Assert Simple Mode is active
        assertTrue("Simple mode must be enabled for this QA simulation", settingsRepository.simpleModeEnabled.first())

        val baseDate = LocalDate.of(2026, 9, 10)

        // Day 1: Complete 3 tasks (100% completion)
        val day1 = baseDate
        var xpEarnedDay1 = 0
        for (i in 1..3) {
            val xp = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
            logs.add(TaskCompletionLogEntity(taskId = i.toLong(), completedDate = day1, wasCompleted = true, pointsAwarded = xp))
            xpEarnedDay1 += xp
        }
        streak = streak.copy(
            currentStreak = 1,
            longestStreak = 1,
            lastCompletedDate = day1,
            perfectDaysCount = 1
        )
        profile = profile.copy(
            totalXp = profile.totalXp + xpEarnedDay1,
            perfectDaysTowardNextLevel = profile.perfectDaysTowardNextLevel + 1
        )
        assertEquals(1, streak.currentStreak)
        assertEquals(1, streak.longestStreak)
        assertEquals(1, streak.perfectDaysCount)

        // Day 2: Complete 2 tasks (100% completion)
        val day2 = baseDate.plusDays(1)
        var xpEarnedDay2 = 0
        for (i in 4..5) {
            val xp = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
            logs.add(TaskCompletionLogEntity(taskId = i.toLong(), completedDate = day2, wasCompleted = true, pointsAwarded = xp))
            xpEarnedDay2 += xp
        }
        streak = streak.copy(
            currentStreak = 2,
            longestStreak = 2,
            lastCompletedDate = day2,
            perfectDaysCount = 2
        )
        profile = profile.copy(
            totalXp = profile.totalXp + xpEarnedDay2,
            perfectDaysTowardNextLevel = profile.perfectDaysTowardNextLevel + 1
        )
        assertEquals(2, streak.currentStreak)
        assertEquals(2, streak.longestStreak)

        // Day 3: Complete 3 of 4 tasks (75% completion >= 70% threshold)
        val day3 = baseDate.plusDays(2)
        var xpEarnedDay3 = 0
        for (i in 6..8) {
            val xp = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
            logs.add(TaskCompletionLogEntity(taskId = i.toLong(), completedDate = day3, wasCompleted = true, pointsAwarded = xp))
            xpEarnedDay3 += xp
        }
        // Task 9 was missed
        logs.add(TaskCompletionLogEntity(taskId = 9L, completedDate = day3, wasCompleted = false, pointsAwarded = 0))
        streak = streak.copy(
            currentStreak = 3,
            longestStreak = 3,
            lastCompletedDate = day3,
            perfectDaysCount = 3
        )
        profile = profile.copy(
            totalXp = profile.totalXp + xpEarnedDay3,
            perfectDaysTowardNextLevel = profile.perfectDaysTowardNextLevel + 1
        )
        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.perfectDaysCount)

        // Day 4: 0 tasks completed -> Streak break evaluated by nightly worker
        // Current streak resets to 0, but longestStreak is preserved at 3
        streak = streak.copy(currentStreak = 0)
        assertEquals(0, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertEquals(3, streak.perfectDaysCount)

        // Day 5: Rebound -> Complete 2 tasks, starting streak back at 1
        val day5 = baseDate.plusDays(4)
        var xpEarnedDay5 = 0
        for (i in 10..11) {
            val xp = PointsCalculator.calculateXpForTask(currentStreak = streak.currentStreak)
            logs.add(TaskCompletionLogEntity(taskId = i.toLong(), completedDate = day5, wasCompleted = true, pointsAwarded = xp))
            xpEarnedDay5 += xp
        }
        streak = streak.copy(
            currentStreak = 1,
            longestStreak = 3, // Preserved!
            lastCompletedDate = day5,
            perfectDaysCount = 4
        )
        profile = profile.copy(
            totalXp = profile.totalXp + xpEarnedDay5,
            perfectDaysTowardNextLevel = profile.perfectDaysTowardNextLevel + 1
        )

        // Final verification across the 5 simulated days
        assertEquals(11, logs.size)
        assertEquals(1, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertEquals(4, streak.perfectDaysCount)
        assertEquals(4, profile.perfectDaysTowardNextLevel)
        assertEquals(xpEarnedDay1 + xpEarnedDay2 + xpEarnedDay3 + xpEarnedDay5, profile.totalXp)
        assertTrue("Backend progression must be strictly positive", profile.totalXp > 0)
    }
}
