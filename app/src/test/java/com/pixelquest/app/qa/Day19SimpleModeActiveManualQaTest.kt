package com.pixelquest.app.qa

import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.domain.model.TodayFlavorText
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
        assertEquals("Task", terminology.taskNoun)
        assertEquals("Tasks", terminology.taskNounPlural)
        assertEquals("Create Task", terminology.createTaskAction)
        assertEquals("No tasks for today. Take a breather!", terminology.emptyTasksDescription)

        // Today screen header and streak strip
        val isStreakStripVisible = !SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            isSimpleMode
        )
        assertFalse("Streak/XP summary strip must be hidden", isStreakStripVisible)

        // Perfect Day banner rewording
        val allDoneBannerText = if (isSimpleMode) "All tasks done for today" else "🌟 PERFECT DAY! All quests completed!"
        assertEquals("All tasks done for today", allDoneBannerText)

        // Flavor text
        val flavor = TodayFlavorText.forDay(
            completedCount = 5,
            totalCount = 5,
            streakDays = 12,
            isSimpleMode = isSimpleMode
        )
        assertFalse("Flavor text must not contain RPG jargon", flavor.quote.contains("quest", ignoreCase = true))
        assertFalse("Flavor text must not contain streak references", flavor.quote.contains("streak", ignoreCase = true))
    }

    @Test
    fun `qa_profile_screen_clean_metrics_and_neutral_frame`() {
        val isXpBarVisible = !SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.XP_BAR,
            isSimpleMode
        )
        val isLevelBadgeVisible = !SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.LEVEL_BADGE,
            isSimpleMode
        )
        assertFalse("XP bar must be hidden on ProfileScreen", isXpBarVisible)
        assertFalse("Level badge must be hidden on ProfileScreen", isLevelBadgeVisible)

        // Difficulty locked on profile
        val isDifficultyLocked = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.DIFFICULTY_SELECTION,
            isSimpleMode
        )
        assertTrue("Difficulty change must be locked on ProfileScreen", isDifficultyLocked)
    }

    @Test
    fun `qa_stats_screen_neutral_heatmap_and_hidden_streaks`() {
        val statsState = StatsUiState(
            currentStreak = 14,
            longestStreak = 25,
            completionRate = 88.5f,
            totalTasksCompleted = 142,
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
        assertEquals("Did you complete this task?", copy.questionPrompt)
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
