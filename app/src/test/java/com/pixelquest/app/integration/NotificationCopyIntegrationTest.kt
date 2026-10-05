package com.pixelquest.app.integration

import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Step 34: Integration test for notification and prompt copy switching
 * dynamically and correctly based on reactive Simple Mode state.
 */
class NotificationCopyIntegrationTest {

    private inner class TestSettingsRepository : SettingsRepository {
        private val _simpleModeFlow = MutableStateFlow(false)
        override val simpleModeEnabled: Flow<Boolean> = _simpleModeFlow.asStateFlow()
        override val isSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isCrtEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val isHapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isReduceMotionEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val onboardingComplete: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = MutableStateFlow(true)

        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}

        override suspend fun setSimpleModeEnabled(enabled: Boolean) {
            _simpleModeFlow.value = enabled
        }
    }

    private lateinit var settingsRepository: SettingsRepository
    private val taskName = "Morning Meditation"

    @Before
    fun setUp() {
        settingsRepository = TestSettingsRepository()
    }

    @Test
    fun testNotificationAndPromptCopy_switchesDynamicallyBasedOnSimpleModeState() = runBlocking {
        // --- PHASE 1: Simple Mode is OFF (Gamified Mode) ---
        assertFalse(settingsRepository.simpleModeEnabled.first())
        val gamifiedMode = settingsRepository.simpleModeEnabled.first()

        val gamifiedReminder = NotificationHelper.getReminderText(taskName, gamifiedMode)
        assertEquals("Did you complete this quest today? Keep your streak!", gamifiedReminder)
        assertTrue(gamifiedReminder.contains("streak", ignoreCase = true))

        // A missed quest's notice no longer claims the streak broke (Day 26).
        val gamifiedMissed = NotificationHelper.getMissedTaskText(taskName, gamifiedMode)
        assertEquals("Morning Meditation slipped past its time. Log it now or start fresh tomorrow.", gamifiedMissed)

        val gamifiedCopy = TaskPromptCopyVariants.resolve(gamifiedMode)
        assertEquals("DID YOU DO IT?", gamifiedCopy.headerTitle)
        assertEquals("YES!", gamifiedCopy.confirmButtonText)
        assertEquals("NOT YET", gamifiedCopy.dismissButtonText)

        // --- PHASE 2: Toggle Simple Mode ON ---
        settingsRepository.setSimpleModeEnabled(true)
        assertTrue(settingsRepository.simpleModeEnabled.first())
        val simpleMode = settingsRepository.simpleModeEnabled.first()

        val simpleReminder = NotificationHelper.getReminderText(taskName, simpleMode)
        assertEquals("Time to complete: Morning Meditation", simpleReminder)
        assertFalse("Simple reminder must not mention streak", simpleReminder.contains("streak", ignoreCase = true))

        val simpleMissed = NotificationHelper.getMissedTaskText(taskName, simpleMode)
        assertEquals("Morning Meditation was due earlier today. You can still log it in the app.", simpleMissed)
        assertFalse("Simple missed notice must not mention streak", simpleMissed.contains("streak", ignoreCase = true))

        val simpleCopy = TaskPromptCopyVariants.resolve(simpleMode)
        assertEquals("TASK REMINDER", simpleCopy.headerTitle)
        assertEquals("Completed", simpleCopy.confirmButtonText)
        assertEquals("Not yet", simpleCopy.dismissButtonText)

        // --- PHASE 3: Toggle Simple Mode back to OFF ---
        settingsRepository.setSimpleModeEnabled(false)
        val revertedMode = settingsRepository.simpleModeEnabled.first()
        assertFalse(revertedMode)

        assertEquals(gamifiedReminder, NotificationHelper.getReminderText(taskName, revertedMode))
        assertEquals(gamifiedMissed, NotificationHelper.getMissedTaskText(taskName, revertedMode))
        assertEquals("DID YOU DO IT?", TaskPromptCopyVariants.resolve(revertedMode).headerTitle)
    }
}
