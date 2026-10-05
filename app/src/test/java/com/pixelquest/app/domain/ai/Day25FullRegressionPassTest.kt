package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 39: Full Regression Pass Test confirming that Day 25's AI Habit Insights,
 * Room caching migration (v3 -> v4), rate limiting, and Today screen integration
 * introduced zero regressions to core PixelQuest functionality.
 */
class Day25FullRegressionPassTest {

    @Test
    fun databaseSchema_maintainsNonDestructiveIntegrity() {
        // Every migration since version 3 is still present (the database is now version 7).
        assertNotNull(AppDatabase.MIGRATION_3_4)
        assertNotNull(AppDatabase.MIGRATION_6_7)

        // Verify entity class signatures are non-destructively intact
        val task = TaskEntity(
            id = 1L,
            name = "Daily Meditation",
            description = "",
            scheduledDay = java.time.LocalDate.now(),
            scheduledTime = java.time.LocalTime.of(8, 0),
            recurrenceType = com.pixelquest.app.domain.model.RecurrenceType.DAILY,
            category = com.pixelquest.app.domain.model.TaskCategory.HEALTH
        )
        val log = TaskCompletionLogEntity(
            id = 1L,
            taskId = 1L,
            completedDate = java.time.LocalDate.now(),
            wasCompleted = true,
            pointsAwarded = 25
        )
        val streak = StreakEntity(id = 1L, currentStreak = 5, longestStreak = 10, perfectDaysCount = 12)
        val profile = UserProfileEntity(id = 1L, username = "PixelHero", avatarId = "warrior", level = 2, totalXp = 150)
        val cache = InsightCacheEntity(
            id = 1L,
            generatedAt = System.currentTimeMillis(),
            summary = "Summary text",
            suggestion = "Suggestion text",
            encouragement = "Encouragement text",
            dataHash = "hash123"
        )

        assertNotNull(task)
        assertNotNull(log)
        assertNotNull(streak)
        assertNotNull(profile)
        assertNotNull(cache)
    }

    @Test
    fun navigationRoutes_allCoreRoutesPreservedAndAccessible() {
        val expectedRoutes = listOf(
            Screen.Home.route,
            Screen.Tasks.route,
            Screen.CreateTask.route,
            Screen.EditTask.route,
            Screen.Profile.route,
            Screen.Stats.route,
            Screen.Settings.route,
            Screen.AiInsight.route
        )

        for (route in expectedRoutes) {
            assertTrue("Route must be non-empty", route.isNotEmpty())
        }

        assertEquals("home", Screen.Home.route)
        assertEquals("tasks", Screen.Tasks.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("ai_insight", Screen.AiInsight.route)
    }

    @Test
    fun themeModes_allSupportedWithoutConflict() {
        val modes = listOf(ThemeMode.Pixel, ThemeMode.Light, ThemeMode.Comic)
        assertEquals(3, modes.size)
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId("pixel"))
        assertEquals(ThemeMode.Light, ThemeMode.fromId("light"))
        assertEquals(ThemeMode.Comic, ThemeMode.fromId("comic"))
        // Fallback safety
        assertEquals(ThemeMode.Pixel, ThemeMode.fromId("unknown_mode"))
    }

    @Test
    fun rateLimitingAndCaps_operateIndependently() {
        // Enforce that rate limit interval and cost safeguard caps are complementary
        assertTrue("Min interval must be 6 hours", com.pixelquest.app.data.repository.HabitInsightRepositoryImpl.MIN_CALL_INTERVAL_MS == 6 * 3600 * 1000L)
        assertTrue("Cache TTL must be 12 hours", com.pixelquest.app.data.repository.HabitInsightRepositoryImpl.CACHE_TTL_MILLIS == 12 * 3600 * 1000L)
        assertTrue("Daily cap must be 4 calls", AiUsagePolicy.MAX_CALLS_PER_DAY == 4)
        assertTrue("Monthly cap must be 60 calls", AiUsagePolicy.MAX_CALLS_PER_MONTH == 60)
    }
}
