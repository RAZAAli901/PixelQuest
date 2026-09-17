package com.pixelquest.app.integration

import com.pixelquest.app.domain.model.TaskPromptCopyVariants
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

        val gamifiedReminder = NotificationHelper.buildReminderContentText(taskName, gamifiedMode)
        assertEquals("Keep your streak! Time to do: Morning Meditation", gamifiedReminder)
        assertTrue(gamifiedReminder.contains("streak", ignoreCase = true))

        val gamifiedMissed = NotificationHelper.buildMissedTaskContentText(taskName, gamifiedMode)
        assertEquals("Don't break your streak! You missed: Morning Meditation", gamifiedMissed)
        assertTrue(gamifiedMissed.contains("break your streak", ignoreCase = true))

        val gamifiedTitle = TaskPromptCopyVariants.getTitle(gamifiedMode)
        assertEquals("⚔️ DID YOU DO IT?", gamifiedTitle)
        assertEquals("YES!", TaskPromptCopyVariants.getConfirmButtonText(gamifiedMode))
        assertEquals("NOT YET", TaskPromptCopyVariants.getDismissButtonText(gamifiedMode))

        // --- PHASE 2: Toggle Simple Mode ON ---
        settingsRepository.setSimpleModeEnabled(true)
        assertTrue(settingsRepository.simpleModeEnabled.first())
        val simpleMode = settingsRepository.simpleModeEnabled.first()

        val simpleReminder = NotificationHelper.buildReminderContentText(taskName, simpleMode)
        assertEquals("Time to do: Morning Meditation", simpleReminder)
        assertFalse("Simple reminder must not mention streak", simpleReminder.contains("streak", ignoreCase = true))

        val simpleMissed = NotificationHelper.buildMissedTaskContentText(taskName, simpleMode)
        assertEquals("You missed a task: Morning Meditation", simpleMissed)
        assertFalse("Simple missed notice must not mention streak", simpleMissed.contains("streak", ignoreCase = true))

        val simpleTitle = TaskPromptCopyVariants.getTitle(simpleMode)
        assertEquals("Did you complete this task?", simpleTitle)
        assertEquals("Completed", TaskPromptCopyVariants.getConfirmButtonText(simpleMode))
        assertEquals("Not yet", TaskPromptCopyVariants.getDismissButtonText(simpleMode))

        // --- PHASE 3: Toggle Simple Mode back to OFF ---
        settingsRepository.setSimpleModeEnabled(false)
        val revertedMode = settingsRepository.simpleModeEnabled.first()
        assertFalse(revertedMode)

        val revertedReminder = NotificationHelper.buildReminderContentText(taskName, revertedMode)
        assertEquals("Keep your streak! Time to do: Morning Meditation", revertedReminder)

        val revertedMissed = NotificationHelper.buildMissedTaskContentText(taskName, revertedMode)
        assertEquals("Don't break your streak! You missed: Morning Meditation", revertedMissed)

        assertEquals("⚔️ DID YOU DO IT?", TaskPromptCopyVariants.getTitle(revertedMode))
    }
}
