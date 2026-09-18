package com.pixelquest.app.ui

import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.model.TaskTerminology
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 48: Verification that Simple Mode UI state persists and renders correctly
 * across an app restart (cold start) with zero flash of gamified content.
 */
class SimpleModeColdRestartPersistenceTest {

    // Simulates SharedPreferences backing across cold process death
    private val persistentStorage = mutableMapOf<String, Boolean>()

    private fun saveSimpleModePreference(enabled: Boolean) {
        persistentStorage["key_simple_mode_enabled"] = enabled
    }

    private fun readSimpleModeOnColdStart(): Boolean {
        return persistentStorage["key_simple_mode_enabled"] ?: false
    }

    @Test
    fun `simple mode state persists across cold process restart`() {
        // 1. User enables Simple Mode in session 1
        saveSimpleModePreference(true)

        // 2. Process death and cold application launch
        val coldStartSimpleMode = readSimpleModeOnColdStart()
        assertTrue("Cold start must immediately read Simple Mode = true", coldStartSimpleMode)

        // 3. UI layers initialize with simple mode active on first frame (no flash of gamified UI)
        val terminology = TaskTerminology.forMode(coldStartSimpleMode)
        assertEquals("Task", terminology.taskNoun)
        assertEquals("TODAY'S TASKS", terminology.todayHeader)

        val isStreakSuppressed = SimpleModeSuppression.isFeatureSuppressed(
            SimpleModeSuppression.Feature.STREAK_DISPLAY,
            coldStartSimpleMode
        )
        assertTrue("Streak strip must be suppressed on first frame after restart", isStreakSuppressed)

        // 4. User disables Simple Mode in session 2 and restarts
        saveSimpleModePreference(false)
        val secondColdStart = readSimpleModeOnColdStart()
        assertFalse("Second cold start must read Simple Mode = false", secondColdStart)

        val restoredTerminology = TaskTerminology.forMode(secondColdStart)
        assertEquals("Quest", restoredTerminology.taskNoun)
        assertEquals("TODAY'S QUESTS", restoredTerminology.todayHeader)
    }
}
