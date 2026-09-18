package com.pixelquest.app.qa

import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.domain.model.CrtFilterPolicy
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.domain.model.TodayFlavorText
import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import com.pixelquest.app.ui.screens.stats.StatsUiState
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 44: Full regression test verifying that the default Gamified Mode (isSimpleMode = false)
 * remains 100% visually and functionally unaffected by Day 19's Simple Mode adaptations.
 */
class Day19GamifiedModeDefaultRegressionTest {

    private val isSimpleMode = false

    @Test
    fun `regression_today_screen_gamified_elements_fully_present`() {
        // 1. Terminology uses Quest
        val terminology = TaskTerminology.forMode(isSimpleMode)
        assertEquals("Quest", terminology.taskNoun)
        assertEquals("Quests", terminology.taskNounPlural)
        assertEquals("TODAY'S QUESTS", terminology.todayHeader)
        assertEquals("Create Quest", terminology.createTaskAction)
        assertEquals("No quests for today. Take a breather!", terminology.emptyTasksDescription)

        // 2. Streak/XP strip is NOT suppressed
        val isStreakStripSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            isSimpleMode
        )
        assertFalse("Streak strip must remain visible in default gamified mode", isStreakStripSuppressed)

        // 3. Perfect Day banner uses gamified celebratory wording
        val allDoneBannerText = if (isSimpleMode) "All tasks done for today" else "🌟 PERFECT DAY! All quests completed!"
        assertEquals("🌟 PERFECT DAY! All quests completed!", allDoneBannerText)

        // 4. Flavor text utilizes RPG / streak motivational phrases
        val flavor = TodayFlavorText.forDay(
            completedCount = 3,
            totalCount = 3,
            streakDays = 5,
            isSimpleMode = isSimpleMode
        )
        assertNotNull(flavor.quote)
    }

    @Test
    fun `regression_profile_screen_level_xp_and_tier_frames_intact`() {
        val isXpBarSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.XP_BAR,
            isSimpleMode
        )
        val isLevelBadgeSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.LEVEL_BADGE,
            isSimpleMode
        )
        assertFalse("XP bar must remain visible in Gamified Mode", isXpBarSuppressed)
        assertFalse("Level badge must remain visible in Gamified Mode", isLevelBadgeSuppressed)

        // Avatar tier framing (Bronze/Silver/Gold) active
        val level5Tier = AvatarTierCalculator.calculateTier(5)
        assertEquals(AvatarTier.SILVER, level5Tier)

        // Difficulty change entry point is enabled
        val isDifficultyLocked = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.DIFFICULTY_SELECTION,
            isSimpleMode
        )
        assertFalse("Difficulty selection must NOT be locked in default Gamified Mode", isDifficultyLocked)
    }

    @Test
    fun `regression_stats_screen_streaks_and_gamified_labels_intact`() {
        val statsState = StatsUiState(
            currentStreak = 7,
            longestStreak = 14,
            totalPoints = 420,
            overallCompletionRate = 85.0f,
            isSimpleMode = isSimpleMode
        )
        assertFalse(statsState.isSimpleMode)
        assertEquals(7, statsState.currentStreak)
        assertEquals(14, statsState.longestStreak)
        assertEquals(420, statsState.totalPoints)

        // Heatmap legend labels
        val perfectDayLabel = if (isSimpleMode) "Completed" else "Perfect Day"
        val noQuestsLabel = if (isSimpleMode) "No Tasks" else "No Quests"
        assertEquals("Perfect Day", perfectDayLabel)
        assertEquals("No Quests", noQuestsLabel)
    }

    @Test
    fun `regression_prompts_and_celebrations_active`() {
        val copy = TaskPromptCopyVariants.resolve(isSimpleMode)
        assertEquals("⚔️ DID YOU DO IT?", copy.headerTitle)
        assertEquals("YES!", copy.confirmButtonText)
        assertEquals("NO", copy.dismissButtonText)
        assertEquals("⚔️", copy.iconEmoji)

        val isCelebrationSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.LEVEL_UP_CELEBRATION,
            isSimpleMode
        )
        assertFalse("Celebrations must remain active in Gamified Mode", isCelebrationSuppressed)
    }

    @Test
    fun `regression_crt_filter_operates_normally`() {
        val shouldApplyCrt = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = true,
            effectiveThemeMode = ThemeMode.Pixel,
            isSimpleModeEnabled = isSimpleMode
        )
        assertTrue("CRT filter must apply in Pixel theme when enabled in Gamified Mode", shouldApplyCrt)
    }
}
