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

        val streakRepo = object : StreakRepository {
            override suspend fun insertStreak(streak: StreakEntity) {}
            override suspend fun updateStreak(streak: StreakEntity) {}
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity())
        }

        val profileRepo = object : UserProfileRepository {
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity(username = "Hero", avatarId = "1"))
        }

        val taskRepo = object : TaskRepository {
            override suspend fun insertTask(task: com.pixelquest.app.data.local.entity.TaskEntity): Long = 1L
            override suspend fun updateTask(task: com.pixelquest.app.data.local.entity.TaskEntity) {}
            override suspend fun deleteTask(task: com.pixelquest.app.data.local.entity.TaskEntity) {}
            override fun getAllTasks(): Flow<List<com.pixelquest.app.data.local.entity.TaskEntity>> = flowOf(emptyList())
            override fun getTaskById(taskId: Long): Flow<com.pixelquest.app.data.local.entity.TaskEntity?> = flowOf(null)
            override fun getTasksForDay(day: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskEntity>> = flowOf(emptyList())
        }

        val completionRepo = object : TaskCompletionRepository {
            override suspend fun insertLog(log: com.pixelquest.app.data.local.entity.TaskCompletionLogEntity): Long = 1L
            override fun getLogsForDate(date: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getLogsForTask(taskId: Long): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getCompletionHistory(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
            override fun getAllLogs(): Flow<List<com.pixelquest.app.data.local.entity.TaskCompletionLogEntity>> = flowOf(emptyList())
        }

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
