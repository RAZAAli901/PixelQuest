package com.pixelquest.app.qa

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.ai.DebugAiInsightTrigger
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 37: Manual QA verification test testing deliberate malformed/unexpected Gemini response payloads.
 * Confirms graceful parsing failure (GeminiResult.MalformedResponse) with zero uncaught crashes
 * across multiple edge-case scenarios (raw conversational text, truncated JSON, HTML errors, missing fields).
 */
class Day24MalformedResponseQaTest {

    private class MockReturningGeminiClient(
        var payloadToReturn: String
    ) : GeminiClient {
        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            return payloadToReturn
        }
    }

    private fun createRepository(geminiClient: GeminiClient): HabitInsightRepositoryImpl {
        val streakRepo = com.pixelquest.app.testing.FakeStreakRepository(StreakEntity())

        val profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(UserProfileEntity(avatarId = "avatar_hero", username = "Hero", ))

        val taskRepo = com.pixelquest.app.testing.FakeTaskRepository(emptyList())

        val completionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository(emptyList())

        val settingsRepo = object : SettingsRepository {
            override val aiInsightsEnabled: Flow<Boolean> = flowOf(true)
            override val simpleModeEnabled: Flow<Boolean> = flowOf(false)
            override val isSoundEnabled: Flow<Boolean> = flowOf(true)
            override val isCrtEnabled: Flow<Boolean> = flowOf(false)
            override val isHapticsEnabled: Flow<Boolean> = flowOf(true)
            override val isReduceMotionEnabled: Flow<Boolean> = flowOf(false)
            override val onboardingComplete: Flow<Boolean> = flowOf(true)
            override val isNotificationsEnabled: Flow<Boolean> = flowOf(true)
            override val isNotificationSoundEnabled: Flow<Boolean> = flowOf(true)
            override val isNotificationVibrationEnabled: Flow<Boolean> = flowOf(true)
            override suspend fun setSoundEnabled(enabled: Boolean) {}
            override suspend fun setCrtEnabled(enabled: Boolean) {}
            override suspend fun setHapticsEnabled(enabled: Boolean) {}
            override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
            override suspend fun setOnboardingComplete(complete: Boolean) {}
            override suspend fun setNotificationsEnabled(enabled: Boolean) {}
            override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
            override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
        }

        return HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = geminiClient,
            toneHook = DefaultHabitInsightToneHook()
        )
    }

    @Test
    fun manualQa_rawConversationalTextResponse_failsGracefullyWithoutCrash() = runBlocking {
        val nonJsonText = "Greetings, brave hero! Here is your quest evaluation for the week. You did very well on fitness quests!"
        val client = MockReturningGeminiClient(nonJsonText)
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()

        assertNotNull("Result must not be null", result)
        assertFalse("Malformed response must not be Success", result.isSuccess)
        assertTrue("Result must be GeminiResult.MalformedResponse", result is GeminiResult.MalformedResponse)

        val malformed = result as GeminiResult.MalformedResponse
        assertEquals(nonJsonText, malformed.rawResponse)
        assertNotNull(malformed.cause)
        assertTrue("Trigger status reflects malformed parse failure", trigger.debugStatus.value.startsWith("MALFORMED"))
        assertNull("latestInsight Flow should remain null", repository.latestInsight.first())
    }

    @Test
    fun manualQa_truncatedJson_failsGracefully() = runBlocking {
        val truncatedJson = """{"summary": "Adventurer, your streak is strong", "sugg"""
        val client = MockReturningGeminiClient(truncatedJson)
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue(result is GeminiResult.MalformedResponse)
        assertTrue(trigger.debugStatus.value.startsWith("MALFORMED"))
    }

    @Test
    fun manualQa_missingRequiredFieldsJson_failsGracefully() = runBlocking {
        val missingFieldsJson = """{"summary": "Good job", "highlightCategory": "FITNESS"}"""
        val client = MockReturningGeminiClient(missingFieldsJson)
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue(result is GeminiResult.MalformedResponse)
        val malformed = result as GeminiResult.MalformedResponse
        assertTrue("Message indicates missing fields", malformed.message.contains("Missing required insight fields"))
    }

    @Test
    fun manualQa_htmlGatewayError_failsGracefully() = runBlocking {
        val html502 = "<html><head><title>502 Bad Gateway</title></head><body><h1>Bad Gateway</h1></body></html>"
        val client = MockReturningGeminiClient(html502)
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue("HTML payload safely caught as MalformedResponse", result is GeminiResult.MalformedResponse)
        assertEquals(html502, (result as GeminiResult.MalformedResponse).rawResponse)
    }

    @Test
    fun manualQa_jsonArrayInsteadOfObject_failsGracefully() = runBlocking {
        val jsonArray = """["summary", "suggestion", "encouragement"]"""
        val client = MockReturningGeminiClient(jsonArray)
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue("JSON array caught as MalformedResponse", result is GeminiResult.MalformedResponse)
    }

    @Test
    fun manualQa_emptyOrBlankResponse_failsGracefully() = runBlocking {
        val emptyString = "   \n\t   "
        val client = MockReturningGeminiClient(emptyString)
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue("Blank response caught as MalformedResponse", result is GeminiResult.MalformedResponse)
    }
}
