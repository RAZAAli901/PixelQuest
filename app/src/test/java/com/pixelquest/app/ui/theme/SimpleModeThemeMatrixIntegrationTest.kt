package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.model.CrtFilterPolicy
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 39: Integration test covering the Simple Mode × Theme Mode combination matrix
 * for Pixel and Light themes (with note for Comic theme extension once Day 23 lands).
 *
 * Matrix:
 * | Theme Mode | Simple Mode | Color Scheme | CRT Overlay | Terminology | Level/Streak UI |
 * |------------|-------------|--------------|-------------|-------------|-----------------|
 * | Pixel      | False       | Dark Pixel   | Allowed     | Quest       | Visible         |
 * | Pixel      | True        | Dark Pixel   | Forced OFF  | Task        | Suppressed      |
 * | Light      | False       | Crisp Light  | Prohibited  | Quest       | Visible         |
 * | Light      | True        | Crisp Light  | Prohibited  | Task        | Suppressed      |
 * | [Comic D23]| [Pending]   | Dynamic      | [TBD]       | [Adapts]    | [Adapts]        |
 */
class SimpleModeThemeMatrixIntegrationTest {

    data class MatrixState(
        val themeMode: ThemeMode,
        val isSimpleMode: Boolean,
        val isCrtSettingEnabled: Boolean
    )

    data class EvaluatedMatrixResult(
        val colorSchemeType: String,
        val shouldApplyCrt: Boolean,
        val taskNoun: String,
        val isStreakSuppressed: Boolean,
        val isDifficultyLocked: Boolean,
        val promptConfirmText: String
    )

    private fun evaluateMatrix(state: MatrixState): EvaluatedMatrixResult {
        val colorSchemeType = when (state.themeMode) {
            ThemeMode.Pixel -> "PixelColorScheme"
            ThemeMode.Light -> "LightColorScheme"
            ThemeMode.System -> "SystemResolved"
        }

        val shouldApplyCrt = CrtFilterPolicy.shouldApplyCrt(
            isCrtSettingEnabled = state.isCrtSettingEnabled,
            effectiveThemeMode = state.themeMode,
            isSimpleModeEnabled = state.isSimpleMode
        )

        val terminology = TaskTerminology.forMode(state.isSimpleMode)
        val copyVariants = TaskPromptCopyVariants.forMode(state.isSimpleMode)

        val isStreakSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            state.isSimpleMode
        )

        val isDifficultyLocked = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.DIFFICULTY_SELECTION,
            state.isSimpleMode
        )

        return EvaluatedMatrixResult(
            colorSchemeType = colorSchemeType,
            shouldApplyCrt = shouldApplyCrt,
            taskNoun = terminology.taskNoun,
            isStreakSuppressed = isStreakSuppressed,
            isDifficultyLocked = isDifficultyLocked,
            promptConfirmText = copyVariants.confirmButton
        )
    }

    @Test
    fun `pixel theme with gamified mode retains full arcade flourishes`() {
        val state = MatrixState(ThemeMode.Pixel, isSimpleMode = false, isCrtSettingEnabled = true)
        val result = evaluateMatrix(state)

        assertEquals("PixelColorScheme", result.colorSchemeType)
        assertTrue("CRT allowed in Pixel theme when gamified", result.shouldApplyCrt)
        assertEquals("Quest", result.taskNoun)
        assertFalse(result.isStreakSuppressed)
        assertFalse(result.isDifficultyLocked)
        assertEquals("YES!", result.promptConfirmText)
    }

    @Test
    fun `pixel theme with simple mode suppresses streaks crt and switches copy`() {
        val state = MatrixState(ThemeMode.Pixel, isSimpleMode = true, isCrtSettingEnabled = true)
        val result = evaluateMatrix(state)

        assertEquals("PixelColorScheme", result.colorSchemeType)
        assertFalse("CRT strictly forced OFF in Simple Mode", result.shouldApplyCrt)
        assertEquals("Task", result.taskNoun)
        assertTrue("Streaks suppressed in Simple Mode", result.isStreakSuppressed)
        assertTrue("Difficulty locked in Simple Mode", result.isDifficultyLocked)
        assertEquals("Completed", result.promptConfirmText)
    }

    @Test
    fun `light theme with gamified mode retains gamified mechanics without crt`() {
        val state = MatrixState(ThemeMode.Light, isSimpleMode = false, isCrtSettingEnabled = true)
        val result = evaluateMatrix(state)

        assertEquals("LightColorScheme", result.colorSchemeType)
        assertFalse("CRT prohibited in Light theme", result.shouldApplyCrt)
        assertEquals("Quest", result.taskNoun)
        assertFalse(result.isStreakSuppressed)
        assertFalse(result.isDifficultyLocked)
        assertEquals("YES!", result.promptConfirmText)
    }

    @Test
    fun `light theme with simple mode cleanly combines crisp styling with minimalist suppression`() {
        val state = MatrixState(ThemeMode.Light, isSimpleMode = true, isCrtSettingEnabled = true)
        val result = evaluateMatrix(state)

        assertEquals("LightColorScheme", result.colorSchemeType)
        assertFalse("CRT prohibited in Light theme", result.shouldApplyCrt)
        assertEquals("Task", result.taskNoun)
        assertTrue("Streaks suppressed in Simple Mode", result.isStreakSuppressed)
        assertTrue("Difficulty locked in Simple Mode", result.isDifficultyLocked)
        assertEquals("Completed", result.promptConfirmText)
    }

    @Test
    fun `switching theme does not mutate or clear simple mode state`() {
        var simpleMode = true
        var currentTheme = ThemeMode.Pixel

        val result1 = evaluateMatrix(MatrixState(currentTheme, simpleMode, isCrtSettingEnabled = true))
        assertTrue(result1.isStreakSuppressed)

        // Switch to Light
        currentTheme = ThemeMode.Light
        val result2 = evaluateMatrix(MatrixState(currentTheme, simpleMode, isCrtSettingEnabled = true))
        assertTrue("Simple Mode remains active after theme switch", result2.isStreakSuppressed)
        assertEquals("Task", result2.taskNoun)
    }

    /**
     * NOTE for Day 23: When Comic theme lands, add:
     * - ThemeMode.Comic × SimpleMode = false (Comic book halftones + gamified terminology)
     * - ThemeMode.Comic × SimpleMode = true (Comic styling with suppressed gamification metrics)
     */
}
