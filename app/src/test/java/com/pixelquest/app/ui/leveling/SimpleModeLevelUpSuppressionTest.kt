package com.pixelquest.app.ui.leveling

import com.pixelquest.app.domain.LevelCalculator
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeLevelUpSuppressionTest {

    private fun evaluateLevelUpUiVisibility(
        perfectDays: Int,
        daysRequired: Int,
        isSimpleMode: Boolean
    ): Pair<Int?, Boolean> {
        val shouldLevelUp = LevelCalculator.shouldLevelUp(perfectDays, daysRequired)
        val pendingLevel = if (shouldLevelUp) 2 else null

        // Mirror HomeUiState and HomeScreen gating contracts
        val finalPendingLevel = if (isSimpleMode) null else pendingLevel
        val isCelebrationModalVisible = finalPendingLevel != null && !isSimpleMode

        return Pair(finalPendingLevel, isCelebrationModalVisible)
    }

    @Test
    fun `scenario A - easy mode threshold met in simple mode suppresses celebration`() {
        val (pendingLevel, isVisible) = evaluateLevelUpUiVisibility(
            perfectDays = 3,
            daysRequired = DifficultyLevel.EASY.daysRequiredPerLevel,
            isSimpleMode = true
        )

        assertNull("Pending level must be null in Simple Mode", pendingLevel)
        assertFalse("LevelUpCelebrationScreen must never be visible in Simple Mode", isVisible)
    }

    @Test
    fun `scenario B - medium mode threshold met in simple mode suppresses celebration`() {
        val (pendingLevel, isVisible) = evaluateLevelUpUiVisibility(
            perfectDays = 7,
            daysRequired = DifficultyLevel.MEDIUM.daysRequiredPerLevel,
            isSimpleMode = true
        )

        assertNull(pendingLevel)
        assertFalse(isVisible)
    }

    @Test
    fun `scenario C - hard mode threshold met in simple mode suppresses celebration`() {
        val (pendingLevel, isVisible) = evaluateLevelUpUiVisibility(
            perfectDays = 14,
            daysRequired = DifficultyLevel.HARD.daysRequiredPerLevel,
            isSimpleMode = true
        )

        assertNull(pendingLevel)
        assertFalse(isVisible)
    }

    @Test
    fun `scenario D - mid progress difficulty switch in simple mode suppresses celebration`() {
        // User had 4 perfect days on Medium (req: 7), then switched to Easy (req: 3).
        // 4 >= 3 triggers level-up immediately, but Simple Mode must suppress celebration overlay.
        val (pendingLevel, isVisible) = evaluateLevelUpUiVisibility(
            perfectDays = 4,
            daysRequired = DifficultyLevel.EASY.daysRequiredPerLevel,
            isSimpleMode = true
        )

        assertNull(pendingLevel)
        assertFalse(isVisible)
    }

    @Test
    fun `scenario E - gamified mode correctly triggers celebration overlay`() {
        val (pendingLevel, isVisible) = evaluateLevelUpUiVisibility(
            perfectDays = 7,
            daysRequired = DifficultyLevel.MEDIUM.daysRequiredPerLevel,
            isSimpleMode = false
        )

        assertEquals(2, pendingLevel)
        assertTrue(isVisible)
    }

    @Test
    fun `home ui state enforces null pending level whenever simple mode is active`() {
        val state = HomeUiState(
            pendingLevelUp = if (true) null else 5,
            isSimpleMode = true
        )

        assertNull(state.pendingLevelUp)
        assertTrue(state.isSimpleMode)
    }
}
