package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 33: Integration test for the full AI habit insight pipeline:
 * gather Room telemetry data -> sanitize & build prompt -> mock Gemini response -> parse into HabitInsightResponse.
 */
class HabitInsightPipelineIntegrationTest {

    private class RecordingMockGeminiClient(
        var responseToReturn: String
    ) : GeminiClient {
        var recordedPrompt: String? = null
        var recordedSystemInstruction: String? = null
        var callCount: Int = 0

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            callCount++
            recordedPrompt = prompt
            recordedSystemInstruction = systemInstruction
            return responseToReturn
        }
    }

    private class FakeSettingsRepository(
        initialAiEnabled: Boolean = true,
        initialSimpleMode: Boolean = false
    ) : SettingsRepository {
        val aiEnabledFlow = MutableStateFlow(initialAiEnabled)
        val simpleModeFlow = MutableStateFlow(initialSimpleMode)

        override val aiInsightsEnabled: Flow<Boolean> = aiEnabledFlow
        override val simpleModeEnabled: Flow<Boolean> = simpleModeFlow

        override suspend fun setAiInsightsEnabled(enabled: Boolean) {
            aiEnabledFlow.value = enabled
        }

        override suspend fun setSimpleModeEnabled(enabled: Boolean) {
            simpleModeFlow.value = enabled
        }

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

    @Test
    fun fullPipeline_gathersData_buildsSanitizedPrompt_callsGemini_andParsesResponse() = runBlocking {
        // 1. Arrange realistic user habit data
        val testStreak = StreakEntity(
            id = 1,
            currentStreak = 7,
            longestStreak = 14,
            perfectDaysCount = 20
        )
        val testProfile = UserProfileEntity(
            id = 1,
            username = "SecretUsernameJohnDoe", // PII to verify exclusion
            avatarId = "wizard_avatar",
            level = 8
        )
        val testTasks = listOf(
            TaskEntity(
                id = 101,
                name = "Private Medical Medication Intake", // Raw PII title to verify exclusion
                description = "Confidential medical prescription",
                scheduledDay = LocalDate.now(),
                scheduledTime = LocalTime.of(8, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.HEALTH
            ),
            TaskEntity(
                id = 102,
                name = "Evening HIIT Sprint Workout",
                description = "Burn calories",
                scheduledDay = LocalDate.now(),
                scheduledTime = LocalTime.of(18, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            ),
            TaskEntity(
                id = 103,
                name = "Read Algorithmic Complexity Book",
                description = "Study chapter 4",
                scheduledDay = LocalDate.now(),
                scheduledTime = LocalTime.of(21, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.LEARNING
            )
        )
        val testLogs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 101, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 2, taskId = 101, completedDate = LocalDate.now().minusDays(1), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 3, taskId = 102, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 4, taskId = 103, completedDate = LocalDate.now(), wasCompleted = false, pointsAwarded = 0)
        )

        val streakRepo = com.pixelquest.app.testing.FakeStreakRepository(testStreak)

        val profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(testProfile)

        val taskRepo = com.pixelquest.app.testing.FakeTaskRepository(testTasks)

        val completionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository(testLogs)

        val settingsRepo = FakeSettingsRepository(initialAiEnabled = true, initialSimpleMode = false)

        val geminiJsonResponse = """
            {
              "summary": "Mighty adventurer, your 7-day streak blazes through Health and Fitness quests with 100% completion!",
              "suggestion": "Bolster your Study questline by scheduling knowledge sessions earlier before mana depletes.",
              "encouragement": "At Level 8, you stand as an inspiration to the guild. Keep the flame alive!",
              "highlightCategory": "HEALTH",
              "specificTaskCallout": "Health habits boast an untarnished completion record"
            }
        """.trimIndent()

        val mockGeminiClient = RecordingMockGeminiClient(geminiJsonResponse)

        val repository = HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = mockGeminiClient,
            toneHook = DefaultHabitInsightToneHook()
        )

        // 2. Act: Execute the complete pipeline
        val result = repository.generateHabitInsight()

        // 3. Assert: Verify prompt building & privacy
        assertEquals("GeminiClient must be called exactly once", 1, mockGeminiClient.callCount)
        val prompt = mockGeminiClient.recordedPrompt
        assertNotNull("Prompt sent to Gemini must not be null", prompt)

        // PII Sanity check: no private task names or usernames in prompt
        assertFalse("Prompt must not contain private task names", prompt!!.contains("Private Medical Medication Intake"))
        assertFalse("Prompt must not contain raw usernames", prompt.contains("SecretUsernameJohnDoe"))
        assertFalse("Prompt must not contain task descriptions", prompt.contains("Confidential medical prescription"))

        // Telemetry check: streak, level, categories present
        assertTrue("Prompt should include streak info", prompt.contains("Current Streak = 7 days"))
        assertTrue("Prompt should include category metrics", prompt.contains("HEALTH") && prompt.contains("FITNESS"))

        // 4. Assert: Verify response parsing
        assertTrue("Result must be GeminiResult.Success", result is GeminiResult.Success)
        val insight = (result as GeminiResult.Success).data
        assertEquals("Mighty adventurer, your 7-day streak blazes through Health and Fitness quests with 100% completion!", insight.summary)
        assertEquals("Bolster your Study questline by scheduling knowledge sessions earlier before mana depletes.", insight.suggestion)
        assertEquals("At Level 8, you stand as an inspiration to the guild. Keep the flame alive!", insight.encouragement)
        assertEquals("HEALTH", insight.highlightCategory)
        assertEquals("Health habits boast an untarnished completion record", insight.specificTaskCallout)

        // 5. Assert: Verify Flow emission
        val latestEmitted = repository.latestInsight.first()
        assertNotNull("Latest insight Flow must emit the parsed insight", latestEmitted)
        assertEquals(insight.summary, latestEmitted?.summary)
    }

    @Test
    fun fullPipeline_simpleMode_adaptsSystemInstructionAndTone() = runBlocking {
        val streakRepo = com.pixelquest.app.testing.FakeStreakRepository(StreakEntity(currentStreak = 3))

        val profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(UserProfileEntity(avatarId = "avatar_hero", username = "Hero", ))

        val taskRepo = com.pixelquest.app.testing.FakeTaskRepository(emptyList())

        val completionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository(emptyList())

        val settingsRepo = FakeSettingsRepository(initialAiEnabled = true, initialSimpleMode = true)

        val geminiResponse = """
            {
              "summary": "You have logged habits for 3 consecutive days.",
              "suggestion": "Keep your daily habits scheduled at consistent times.",
              "encouragement": "Small consistent efforts create lasting routines.",
              "highlightCategory": null,
              "specificTaskCallout": null
            }
        """.trimIndent()

        val mockGeminiClient = RecordingMockGeminiClient(geminiResponse)

        val repository = HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = mockGeminiClient,
            toneHook = DefaultHabitInsightToneHook()
        )

        val result = repository.generateHabitInsight()
        assertTrue(result is GeminiResult.Success)

        val systemInstruction = mockGeminiClient.recordedSystemInstruction
        assertNotNull("System instruction must be passed to client", systemInstruction)
        assertTrue("Simple Mode must use habit coach tone", systemInstruction!!.contains("empathetic habit coach"))
        assertFalse("Simple Mode must not mention the RPG questmaster", systemInstruction.contains("RPG questmaster"))
    }
}
