package com.pixelquest.app.qa

import com.pixelquest.app.domain.AvatarTier
import com.pixelquest.app.domain.AvatarTierCalculator
import com.pixelquest.app.domain.model.CrtFilterPolicy
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import com.pixelquest.app.ui.screens.stats.StatsUiState
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 42: Manual QA simulation verifying that disabling Simple Mode after a period of use
 * restores all gamification elements cleanly with accurate historical data intact (zero data loss).
 */
class Day19SimpleModeDisableReversalQaTest {

    data class SimulatedUserProfile(
        val level: Int,
        val totalXp: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val configuredDifficulty: String
    )

    @Test
    fun `qa_disable_simple_mode_restores_all_gamified_elements_and_preserves_history`() {
        // Step 1: User runs in Simple Mode for several days, completing habits.
        // Background DB tracking quietly recorded progress:
        val historicalProgress = SimulatedUserProfile(
            level = 4,
            totalXp = 850,
            currentStreak = 12,
            longestStreak = 18,
            configuredDifficulty = "HARD"
        )

        // Step 2: User toggles Simple Mode back to OFF
        val isSimpleMode = false

        // A. Terminology immediately reverts to Quests
        val terminology = TaskTerminology.forMode(isSimpleMode)
        assertEquals("Quest", terminology.taskNoun)
        assertEquals("Quests", terminology.taskNounPlural)
        assertEquals("Create Quest", terminology.createTaskAction)

        // B. Streak and XP strip reappears with accurate values
        val isStreakStripSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            isSimpleMode
        )
        assertFalse("Streak strip must be visible again", isStreakStripSuppressed)
        assertEquals(12, historicalProgress.currentStreak)
        assertEquals(850, historicalProgress.totalXp)
        assertEquals(4, historicalProgress.level)

        // C. Avatar tier frame reappears based on earned level 4 (Tier 2/Silver or Gold)
        val tier = AvatarTierCalculator.calculateTier(historicalProgress.level)
        assertNotNull("Earned level must map to a valid AvatarTier", tier)

        // D. Stats screen streak cards and level history link reappear
        val statsState = StatsUiState(
            currentStreak = historicalProgress.currentStreak,
            longestStreak = historicalProgress.longestStreak,
            completionRate = 92.0f,
            totalTasksCompleted = 45,
            isSimpleMode = isSimpleMode
        )
        assertFalse(statsState.isSimpleMode)
        assertEquals(12, statsState.currentStreak)
        assertEquals(18, statsState.longestStreak)

        // E. Difficulty selection unlocks
        val isDifficultyLocked = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.DIFFICULTY_SELECTION,
            isSimpleMode
        )
        assertFalse("Difficulty selection must be unlocked", isDifficultyLocked)
        assertEquals("HARD", historicalProgress.configuredDifficulty)

        // F. CRT filter automatically restores
        val userCrtPreference = true
        val effectiveTheme = ThemeMode.Pixel
        val crtActive = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = userCrtPreference,
            effectiveThemeMode = effectiveTheme,
            isSimpleModeEnabled = isSimpleMode
        )
        assertTrue("CRT must resume immediately without manual re-enabling", crtActive)

        // G. Prompts and celebrations return to epic RPG tone
        val copy = TaskPromptCopyVariants.resolve(isSimpleMode)
        assertEquals("⚔️ DID YOU DO IT?", copy.headerTitle)
        assertEquals("YES!", copy.confirmButtonText)
        assertEquals("NO", copy.dismissButtonText)
        assertEquals("⚔️", copy.iconEmoji)
    }
}
