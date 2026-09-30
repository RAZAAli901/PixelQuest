package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiApiException
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiNetworkException
import com.pixelquest.app.data.remote.GeminiRateLimitException
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.safeGeminiCall
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * Step 34: Tests verifying that the safeGeminiCall error-handling wrapper and HabitInsightRepository
 * correctly surface rate-limit (HTTP 429 / GeminiRateLimitException) and network-failure
 * (IOException / Timeout / GeminiNetworkException) cases without crashing.
 */
class GeminiErrorHandlingTest {

    @Test
    fun safeGeminiCall_surfacesRateLimitException_withoutCrashing() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiRateLimitException(retryAfterSeconds = 60, message = "Quota exceeded (429)")
        }

        assertFalse("Rate limit must not be Success", result.isSuccess)
        assertNull("getOrNull must return null on failure", result.getOrNull())
        assertTrue("Result must be GeminiResult.RateLimited", result is GeminiResult.RateLimited)

        val rateLimit = result as GeminiResult.RateLimited
        assertEquals(60L, rateLimit.retryAfterSeconds)
        assertTrue(rateLimit.message.contains("Quota exceeded"))
    }

    @Test
    fun safeGeminiCall_surfacesHttp429ApiException_asRateLimited() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiApiException(statusCode = 429, message = "Resource exhausted: rate limit exceeded")
        }

        assertTrue("HTTP 429 must be classified as RateLimited", result is GeminiResult.RateLimited)
        val rateLimit = result as GeminiResult.RateLimited
        assertTrue(rateLimit.message.contains("429"))
    }

    @Test
    fun safeGeminiCall_surfacesNetworkIOException_asNetworkError() = runBlocking {
        val result = safeGeminiCall {
            throw IOException("Failed to connect to generativelanguage.googleapis.com")
        }

        assertTrue("IOException must be mapped to NetworkError", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertNotNull(networkError.cause)
        assertTrue(networkError.message.contains("Network error"))
    }

    @Test
    fun safeGeminiCall_surfacesGeminiNetworkException_asNetworkError() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiNetworkException("SSL handshake failed", IOException("Connection reset"))
        }

        assertTrue("GeminiNetworkException must be mapped to NetworkError", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertEquals("SSL handshake failed", networkError.message)
    }

    @Test
    fun safeGeminiCall_surfacesTimeoutException_asNetworkError() = runBlocking {
        val result = safeGeminiCall(timeoutMs = 50L) {
            kotlinx.coroutines.delay(200L)
            "Done"
        }

        assertTrue("Timeout must be mapped to NetworkError", result is GeminiResult.NetworkError)
        val timeoutError = result as GeminiResult.NetworkError
        assertTrue(timeoutError.message.contains("timed out"))
    }

    @Test
    fun safeGeminiCall_surfacesHttp500ApiError_asApiError() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiApiException(statusCode = 500, message = "Internal Generative AI Server Error")
        }

        assertTrue("HTTP 500 must be mapped to ApiError", result is GeminiResult.ApiError)
        val apiError = result as GeminiResult.ApiError
        assertEquals(500, apiError.statusCode)
        assertTrue(apiError.message.contains("Internal Generative AI Server Error"))
    }

    @Test
    fun safeGeminiCall_rethrowsCancellationException_forStructuredConcurrency() = runBlocking {
        try {
            safeGeminiCall {
                throw CancellationException("Coroutine was cancelled")
            }
            fail("CancellationException must be rethrown and never swallowed")
        } catch (e: CancellationException) {
            assertEquals("Coroutine was cancelled", e.message)
        }
    }

    @Test
    fun habitInsightRepository_surfacesRateLimit_throughEndToEndPipeline() = runBlocking {
        val rateLimitedClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
                throw GeminiRateLimitException(retryAfterSeconds = 120, message = "RPM quota exhausted")
            }
        }

        val repository = createTestRepository(rateLimitedClient)
        val result = repository.generateHabitInsight()

        assertTrue("Pipeline must return RateLimited", result is GeminiResult.RateLimited)
        val rateLimited = result as GeminiResult.RateLimited
        assertEquals(120L, rateLimited.retryAfterSeconds)
        assertTrue(rateLimited.message.contains("RPM quota exhausted"))
    }

    @Test
    fun habitInsightRepository_surfacesNetworkError_throughEndToEndPipeline() = runBlocking {
        val offlineClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
                throw IOException("Unable to resolve host: generativelanguage.googleapis.com")
            }
        }

        val repository = createTestRepository(offlineClient)
        val result = repository.generateHabitInsight()

        assertTrue("Pipeline must return NetworkError on connectivity failure", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertTrue(networkError.message.contains("Network error"))
    }

    private fun createTestRepository(geminiClient: GeminiClient): HabitInsightRepositoryImpl {
        val streakRepo = object : StreakRepository {
            override suspend fun insertStreak(streak: StreakEntity) {}
            override suspend fun updateStreak(streak: StreakEntity) {}
            override fun getCurrentStreak(): Flow<StreakEntity?> = flowOf(StreakEntity())
        }

        val profileRepo = object : UserProfileRepository {
            override suspend fun insertProfile(profile: UserProfileEntity) {}
            override suspend fun updateProfile(profile: UserProfileEntity) {}
            override fun getProfile(): Flow<UserProfileEntity?> = flowOf(UserProfileEntity())
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
}
