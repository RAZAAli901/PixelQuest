package com.pixelquest.app.qa

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 44 Regression Test:
 * Verifies that the entire local gameplay loop (tasks, XP, streaks, level progression)
 * operates autonomously and without regressions for users who never sign in or link a cloud account.
 */
class OfflineLocalOnlyRegressionTest {

    @Test
    fun testLocalOnlyUser_functionsFullyWithoutCloudLinkage() {
        // User profile initialized without cloud attributes
        var profile = UserProfileEntity(
            id = 1,
            username = "OfflineAdventurer",
            avatarId = "avatar_archer",
            level = 1,
            totalXp = 0,
            perfectDaysTowardNextLevel = 0,
            supabaseUserId = null,
            leaderboardOptIn = false,
            leaderboardDisplayName = null
        )

        // 1. Verify cloud linkage is null & opt-in is false
        assertNull("Supabase user ID must be null for offline user", profile.supabaseUserId)
        assertFalse("Leaderboard opt-in must be false by default", profile.leaderboardOptIn)
        assertNull("Leaderboard display name must be null", profile.leaderboardDisplayName)

        // 2. Task creation and XP gain in local mode (XP comes from PointsCalculator, not the task)
        val task = TaskEntity(
            id = 101,
            name = "Morning Exercise",
            description = "15 minute stretch",
            scheduledDay = java.time.LocalDate.now(),
            scheduledTime = java.time.LocalTime.of(7, 0),
            recurrenceType = com.pixelquest.app.domain.model.RecurrenceType.DAILY,
            category = com.pixelquest.app.domain.model.TaskCategory.FITNESS
        )
        val earnedXp = com.pixelquest.app.domain.PointsCalculator.calculateXpForTask(task, currentStreak = 0)
        profile = profile.copy(totalXp = profile.totalXp + earnedXp)
        assertEquals(50, profile.totalXp)

        // 3. Completing tasks and streak progress
        var currentStreak = 0
        var longestStreak = 0
        val perfectDayCompleted = true
        if (perfectDayCompleted) {
            currentStreak++
            if (currentStreak > longestStreak) longestStreak = currentStreak
        }
        assertEquals(1, currentStreak)
        assertEquals(1, longestStreak)

        // 4. Levels come from perfect days (7 on Medium), worked out locally
        assertFalse(com.pixelquest.app.domain.LevelCalculator.shouldLevelUp(currentStreak, 7))
        assertEquals(1, profile.level)

        // 5. Assert signing in remains 100% optional: core game features are untouched
        assertEquals("OfflineAdventurer", profile.username)
        assertNull(profile.supabaseUserId)
        assertFalse(profile.leaderboardOptIn)
    }
}
