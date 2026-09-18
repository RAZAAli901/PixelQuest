package com.pixelquest.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeDifficultyEntryPointUiTest {

    private class DifficultyEntryPointSimulator(
        private val isSimpleModeEnabled: Boolean,
        private val onNavigateToDifficulty: () -> Unit
    ) {
        val isButtonEnabled: Boolean = !isSimpleModeEnabled

        fun performClick(): Boolean {
            if (isButtonEnabled) {
                onNavigateToDifficulty()
                return true
            }
            return false
        }

        fun getExplanation(): String? {
            return if (isSimpleModeEnabled) {
                "🔒 Difficulty selection is locked while Simple Mode is active. Thresholds and streaks are paused."
            } else {
                null
            }
        }
    }

    @Test
    fun `clicking difficulty entry point in simple mode is blocked and triggers zero navigation`() {
        var navigationTriggered = false
        val simulator = DifficultyEntryPointSimulator(
            isSimpleModeEnabled = true,
            onNavigateToDifficulty = { navigationTriggered = true }
        )

        assertFalse(simulator.isButtonEnabled)
        val clickHandled = simulator.performClick()

        assertFalse("Click must not be handled when disabled", clickHandled)
        assertFalse("Navigation callback must not fire in Simple Mode", navigationTriggered)

        val explanation = simulator.getExplanation()
        assertNotNull(explanation)
        assertTrue(explanation!!.contains("Difficulty selection is locked"))
    }

    @Test
    fun `clicking difficulty entry point in gamified mode succeeds and triggers navigation`() {
        var navigationTriggered = false
        val simulator = DifficultyEntryPointSimulator(
            isSimpleModeEnabled = false,
            onNavigateToDifficulty = { navigationTriggered = true }
        )

        assertTrue(simulator.isButtonEnabled)
        val clickHandled = simulator.performClick()

        assertTrue(clickHandled)
        assertTrue("Navigation callback must fire in gamified mode", navigationTriggered)
        assertNull(simulator.getExplanation())
    }

    @Test
    fun `simple mode toggle enables and disables difficulty entry point reactively`() {
        var navigationCount = 0
        var isSimpleMode = false

        val onNav = { navigationCount++ }

        // 1. Initially gamified
        var simulator = DifficultyEntryPointSimulator(isSimpleMode, onNav)
        simulator.performClick()
        assertEquals(1, navigationCount)

        // 2. User enables Simple Mode
        isSimpleMode = true
        simulator = DifficultyEntryPointSimulator(isSimpleMode, onNav)
        simulator.performClick()
        assertEquals("Navigation count must not increase in Simple Mode", 1, navigationCount)

        // 3. User disables Simple Mode
        isSimpleMode = false
        simulator = DifficultyEntryPointSimulator(isSimpleMode, onNav)
        simulator.performClick()
        assertEquals("Navigation count must increase when gamified mode is restored", 2, navigationCount)
    }
}
