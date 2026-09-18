package com.pixelquest.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeDifficultyDisabledStateTest {

    data class DifficultyOptionUiModel(
        val isButtonVisible: Boolean,
        val isButtonEnabled: Boolean,
        val buttonAlpha: Float,
        val explanatoryText: String?
    )

    private fun resolveDifficultyOptionUi(isSimpleMode: Boolean): DifficultyOptionUiModel {
        val isButtonVisible = true // Must never vanish
        val isButtonEnabled = !isSimpleMode
        val buttonAlpha = if (!isButtonEnabled) 0.5f else 1.0f
        val explanation = if (isSimpleMode) {
            "🔒 Difficulty selection is locked while Simple Mode is active. Thresholds and streaks are paused."
        } else {
            null
        }

        return DifficultyOptionUiModel(
            isButtonVisible = isButtonVisible,
            isButtonEnabled = isButtonEnabled,
            buttonAlpha = buttonAlpha,
            explanatoryText = explanation
        )
    }

    @Test
    fun `difficulty button is visible with proper disabled alpha and explanation in simple mode`() {
        val ui = resolveDifficultyOptionUi(isSimpleMode = true)

        assertTrue("Difficulty button must remain visible to avoid broken layouts", ui.isButtonVisible)
        assertFalse("Difficulty button must be disabled in Simple Mode", ui.isButtonEnabled)
        assertEquals("Disabled button must use 0.5 alpha styling", 0.5f, ui.buttonAlpha, 0.001f)
        assertNotNull("Explanatory text must be present", ui.explanatoryText)
        assertTrue(ui.explanatoryText!!.contains("Difficulty selection is locked"))
    }

    @Test
    fun `difficulty button is fully enabled and active in gamified mode`() {
        val ui = resolveDifficultyOptionUi(isSimpleMode = false)

        assertTrue(ui.isButtonVisible)
        assertTrue("Difficulty button must be enabled in gamified mode", ui.isButtonEnabled)
        assertEquals(1.0f, ui.buttonAlpha, 0.001f)
        assertNull("Explanatory text must not appear in gamified mode", ui.explanatoryText)
    }
}
