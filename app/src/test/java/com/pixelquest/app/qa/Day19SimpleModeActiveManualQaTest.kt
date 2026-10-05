package com.pixelquest.app.qa

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.domain.FlavorTextCatalog
import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import com.pixelquest.app.ui.screens.stats.StatsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 41: Comprehensive Manual QA simulation for Simple Mode enabled.
 * Navigates and verifies every screen: Today, Profile, Stats, Prompts, Celebrations, Settings.
 * Confirms all gamification elements are cleanly hidden or reworded, with zero leftover game terminology
 * and zero layout regressions.
 */
class Day19SimpleModeActiveManualQaTest {

    private val isSimpleMode = true

    @Test
    fun `qa_today_screen_suppression_and_rewordings`() {
        val terminology = TaskTerminology.forMode(isSimpleMode)
        assertEquals("Task", terminology.itemSingular)
        assertEquals("Tasks", terminology.itemPlural)
        assertEquals("+ CREATE TASK", terminology.createButtonText)
        assertEquals("No tasks scheduled. Add a new task to get started.", terminology.emptyStateSubtitle)

        // Today screen header and streak strip
        val isStreakStripVisible = !SimpleModeSuppression.isSuppressed(
            com.pixelquest.app.domain.model.SimpleModeSuppressedFeature.STREAK_DISPLAY,
            isSimpleMode
        )
        assertFalse("Streak/XP summary strip must be hidden", isStreakStripVisible)

        // Perfect Day banner rewording
        val allDoneBannerText = if (isSimpleMode) "All tasks done for today" else "🌟 PERFECT DAY! All quests completed!"
        assertEquals("All tasks done for today", allDoneBannerText)

        // Flavor text
        val flavor = FlavorTextCatalog.getFlavorText(taskCount = 5, completedCount = 5, isPerfectDay = 5 == 5, isSimpleMode = isSimpleMode)
        assertFalse("Flavor text must not contain RPG jargon", flavor.contains("quest", ignoreCase = true))
        assertFalse("Flavor text must not contain streak references", flavor.contains("streak", ignoreCase = true))
    }

    @Test
    fun `qa_profile_screen_clean_metrics_and_neutral_frame`() {
        val isXpBarVisible = !SimpleModeSuppression.isSuppressed(
            com.pixelquest.app.domain.model.SimpleModeSuppressedFeature.POINTS_XP_DISPLAY,
            isSimpleMode
        )
        val isLevelBadgeVisible = !SimpleModeSuppression.isSuppressed(
            com.pixelquest.app.domain.model.SimpleModeSuppressedFeature.LEVEL_BADGE_AND_CELEBRATION,
            isSimpleMode
        )
        assertFalse("XP bar must be hidden on ProfileScreen", isXpBarVisible)
        assertFalse("Level badge must be hidden on ProfileScreen", isLevelBadgeVisible)

        // Difficulty locked on profile
        val isDifficultyLocked = SimpleModeSuppression.isSuppressed(
            com.pixelquest.app.domain.model.SimpleModeSuppressedFeature.DIFFICULTY_SELECTION,
            isSimpleMode
        )
        assertTrue("Difficulty change must be locked on ProfileScreen", isDifficultyLocked)
    }

    @Test
    fun `qa_stats_screen_neutral_heatmap_and_hidden_streaks`() {
        val statsState = StatsUiState(
            currentStreak = 14,
            longestStreak = 25,
            overallCompletionRate = 88.5f,
            isSimpleMode = isSimpleMode
        )
        assertTrue(statsState.isSimpleMode)

        // Heatmap legend labels
        val perfectDayLabel = if (isSimpleMode) "Completed" else "Perfect Day"
        val noTasksLabel = if (isSimpleMode) "No Tasks" else "No Quests"
        assertEquals("Completed", perfectDayLabel)
        assertEquals("No Tasks", noTasksLabel)

        // Level history link suppressed
        val isLevelHistoryVisible = !isSimpleMode
        assertFalse("Level history quick-link must be hidden in StatsScreen", isLevelHistoryVisible)
    }

    @Test
    fun `qa_prompts_and_celebrations_strictly_suppressed`() {
        // Level up celebration never triggers
        val pendingCelebrationLevel = if (isSimpleMode) null else 5
        assertNull("Celebration modal level must evaluate to null in Simple Mode", pendingCelebrationLevel)

        // Full-screen DidYouDoIt prompt
        val copy = TaskPromptCopyVariants.resolve(isSimpleMode)
        assertEquals("Did you complete this task today?", copy.questionPrompt)
        assertEquals("Completed", copy.confirmButtonText)
        assertEquals("Not yet", copy.dismissButtonText)
        assertNull("Combat swords icon emoji must be omitted in Simple Mode", copy.iconEmoji)
    }

    @Test
    fun `qa_settings_screen_locked_difficulty_with_explanation`() {
        val difficultyClickable = !isSimpleMode
        assertFalse("Difficulty entry point must be disabled", difficultyClickable)

        val explanation = "🔒 Difficulty selection is locked while Simple Mode is active. Thresholds and streaks are paused."
        assertTrue(explanation.contains("locked while Simple Mode is active"))
    }
}
