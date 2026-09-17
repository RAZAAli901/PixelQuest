package com.pixelquest.app.integration

import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightTone
import com.pixelquest.app.domain.ai.HabitInsightToneHook
import com.pixelquest.app.domain.repository.SettingsRepository
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
 * Step 36: Integration test for the AI-insights placeholder hook's interaction
 * with reactive Simple Mode state, ensuring full forward-compatibility for Days 24-25.
 */
class AiInsightsPlaceholderIntegrationTest {

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
    private lateinit var toneHook: HabitInsightToneHook

    @Before
    fun setUp() {
        settingsRepository = TestSettingsRepository()
        toneHook = DefaultHabitInsightToneHook()
    }

    @Test
    fun testAiInsightTone_reactivelyAdaptsToSettingsRepositorySimpleModeState() = runBlocking {
        // 1. Initial State: Gamified Mode (simpleModeEnabled = false)
        val initialMode = settingsRepository.simpleModeEnabled.first()
        assertFalse(initialMode)

        val initialTone = toneHook.resolveTone(initialMode)
        assertEquals(HabitInsightTone.GAMIFIED_HEROIC, initialTone)

        val heroicPrompt = toneHook.getSystemPromptGuidance(initialTone)
        assertTrue(heroicPrompt.contains("RPG", ignoreCase = true))
        assertTrue(heroicPrompt.contains("hero", ignoreCase = true))

        // 2. Transition: Enable Simple Mode
        settingsRepository.setSimpleModeEnabled(true)
        val updatedMode = settingsRepository.simpleModeEnabled.first()
        assertTrue(updatedMode)

        val updatedTone = toneHook.resolveTone(updatedMode)
        assertEquals(HabitInsightTone.SIMPLE_MINIMALIST, updatedTone)

        val minimalistPrompt = toneHook.getSystemPromptGuidance(updatedTone)
        assertTrue(minimalistPrompt.contains("minimalist", ignoreCase = true))
        assertTrue(minimalistPrompt.contains("Do not reference points", ignoreCase = true))

        // 3. Transition: Switch back to Full Game Mode
        settingsRepository.setSimpleModeEnabled(false)
        val revertedMode = settingsRepository.simpleModeEnabled.first()
        assertFalse(revertedMode)

        val revertedTone = toneHook.resolveTone(revertedMode)
        assertEquals(HabitInsightTone.GAMIFIED_HEROIC, revertedTone)
    }
}
