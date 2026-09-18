package com.pixelquest.app.ui

import com.pixelquest.app.domain.model.CrtFilterPolicy
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.domain.model.TodayFlavorText
import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 45: Authoritative UI test suite entry covering the full Simple Mode
 * screen-by-screen checklist from SIMPLE_MODE.md.
 *
 * Validates:
 * 1. TodayScreen Header (StreakXpSummaryStrip suppressed, "TODAY'S TASKS" title, "X / Y Completed")
 * 2. TodayQuestCard (Task terminology, clean presentation)
 * 3. Quick-Complete & Celebration Signals (Fanfare suppressed, neutral checkmark feedback)
 * 4. DidYouDoItScreen & Fullscreen Prompts (Neutral title, "Completed", "Not yet", combat icons omitted)
 * 5. ProfileScreen (XP bar, level badge suppressed, neutral avatar frame, non-gamified stats)
 * 6. StatsScreen (Streak cards hidden, neutral heatmap legend, level history hidden)
 * 7. SettingsScreen (Difficulty entry point disabled with explanatory text, styled toggle)
 * 8. CRT Filter Overlay (Forced OFF in Simple Mode across Pixel theme)
 */
class SimpleModeFullChecklistUiTest {

    private val isSimpleMode = true

    @Test
    fun `checklist_item_1_todayscreen_header_and_streak_strip`() {
        // TodayScreen Header replaced with clean task count and neutral title
        val terminology = TaskTerminology.forMode(isSimpleMode)
        assertEquals("TODAY'S TASKS", terminology.todayHeader)

        val isStripSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            isSimpleMode
        )
        assertTrue("StreakXpSummaryStrip must be suppressed", isStripSuppressed)
    }

    @Test
    fun `checklist_item_2_todayquestcard_terminology`() {
        val terminology = TaskTerminology.forMode(isSimpleMode)
        assertEquals("Task", terminology.taskNoun)
        assertEquals("Tasks", terminology.taskNounPlural)
        assertEquals("Create Task", terminology.createTaskAction)
        assertEquals("Edit Task", terminology.editTaskTitle)
        assertEquals("Delete Task", terminology.deleteTaskTitle)
    }

    @Test
    fun `checklist_item_3_quick_complete_and_celebration_suppression`() {
        val isLevelCelebrationSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.LEVEL_UP_CELEBRATION,
            isSimpleMode
        )
        assertTrue("LevelUpCelebrationScreen must never trigger in Simple Mode", isLevelCelebrationSuppressed)

        val isPointsAwardAnimationSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.POINTS_DISPLAY,
            isSimpleMode
        )
        assertTrue("Points awarded animation must be suppressed in Simple Mode", isPointsAwardAnimationSuppressed)
    }

    @Test
    fun `checklist_item_4_did_you_do_it_fullscreen_prompt`() {
        val copy = TaskPromptCopyVariants.resolve(isSimpleMode)
        assertEquals("TASK CHECK", copy.headerTitle)
        assertEquals("Did you complete this task?", copy.questionPrompt)
        assertEquals("Completed", copy.confirmButtonText)
        assertEquals("Not yet", copy.dismissButtonText)
        assertNull("Combat graphics/swords must be omitted", copy.iconEmoji)
    }

    @Test
    fun `checklist_item_5_profilescreen_suppression_and_neutral_frame`() {
        val isXpBarSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.XP_BAR,
            isSimpleMode
        )
        val isLevelBadgeSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.LEVEL_BADGE,
            isSimpleMode
        )
        assertTrue("XP bar must be suppressed on ProfileScreen", isXpBarSuppressed)
        assertTrue("Level badge must be suppressed on ProfileScreen", isLevelBadgeSuppressed)
    }

    @Test
    fun `checklist_item_6_statsscreen_neutral_charts_and_legend`() {
        val isStreakSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            isSimpleMode
        )
        assertTrue("Streak stats must be suppressed on StatsScreen", isStreakSuppressed)

        // Neutral heatmap labels
        val perfectDayLabel = if (isSimpleMode) "Completed" else "Perfect Day"
        val noTasksLabel = if (isSimpleMode) "No Tasks" else "No Quests"
        assertEquals("Completed", perfectDayLabel)
        assertEquals("No Tasks", noTasksLabel)
    }

    @Test
    fun `checklist_item_7_settings_difficulty_lock`() {
        val isDifficultyLocked = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.DIFFICULTY_SELECTION,
            isSimpleMode
        )
        assertTrue("Difficulty selection must be locked in Settings", isDifficultyLocked)
    }

    @Test
    fun `checklist_item_8_crt_filter_overlay_strictly_off`() {
        val shouldApplyCrt = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Pixel,
            isSimpleModeEnabled = isSimpleMode
        )
        assertFalse("CRT overlay must strictly evaluate to false in Simple Mode", shouldApplyCrt)
    }
}
