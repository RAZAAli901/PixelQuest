package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 28: Unit test verifying that opt-in gating logic in HabitInsightRepository
 * strictly prevents any Gemini network calls when aiInsightsEnabled is false,
 * and allows calls only when explicitly enabled.
 */
class AiInsightsOptInGatingTest {

    private class CallCountingGeminiClient : GeminiClient {
        var callCount = 0

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            callCount++
            return """
                {
                  "summary": "Great consistency on active habits.",
                  "suggestion": "Keep up your daily cadence.",
                  "encouragement": "Victory awaits!",
                  "highlightCategory": "FITNESS",
                  "specificTaskCallout": null
                }
            """.trimIndent()
        }
    }

    private class TestSettingsRepository : SettingsRepository {
        val aiEnabledFlow = MutableStateFlow(false)

        override val aiInsightsEnabled: Flow<Boolean> = aiEnabledFlow

        override suspend fun setAiInsightsEnabled(enabled: Boolean) {
            aiEnabledFlow.value = enabled
        }

        override val isSoundEnabled: Flow<Boolean> = flowOf(true)
        override val isCrtEnabled: Flow<Boolean> = flowOf(false)
        override val isHapticsEnabled: Flow<Boolean> = flowOf(true)
        override val isReduceMotionEnabled: Flow<Boolean> = flowOf(false)
        override val onboardingComplete: Flow<Boolean> = flowOf(true)
        override val isNotificationsEnabled: Flow<Boolean> = flowOf(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = flowOf(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = flowOf(true)
        override val simpleModeEnabled: Flow<Boolean> = flowOf(false)

        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
    }

    @Test
    fun optInGating_preventsCallsWhenDisabled() = runBlocking {
        val mockClient = CallCountingGeminiClient()
        val settingsRepo = TestSettingsRepository()
        settingsRepo.setAiInsightsEnabled(false)

        val streakRepo = com.pixelquest.app.testing.FakeStreakRepository(StreakEntity())

        val profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(UserProfileEntity(username = "Hero", avatarId = "1"))

        val taskRepo = com.pixelquest.app.testing.FakeTaskRepository()

        val completionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository()

        val repository = HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = mockClient,
            toneHook = DefaultHabitInsightToneHook()
        )

        // 1. Initial invocation with opt-in = false
        val disabledResult = repository.generateHabitInsight()
        assertNotNull(disabledResult)
        assertTrue("Result should be GeminiResult.Disabled", disabledResult is GeminiResult.Disabled)
        assertEquals("Client should not have been called", 0, mockClient.callCount)

        // 2. Enable AI insights
        settingsRepo.setAiInsightsEnabled(true)
        val enabledResult = repository.generateHabitInsight()
        assertTrue("Result should be GeminiResult.Success", enabledResult is GeminiResult.Success)
        assertEquals("Client should have been called exactly once", 1, mockClient.callCount)

        // 3. Disable AI insights again
        settingsRepo.setAiInsightsEnabled(false)
        val disabledAgainResult = repository.generateHabitInsight()
        assertTrue("Result should be GeminiResult.Disabled", disabledAgainResult is GeminiResult.Disabled)
        assertEquals("Client call count should remain 1", 1, mockClient.callCount)
    }
}
