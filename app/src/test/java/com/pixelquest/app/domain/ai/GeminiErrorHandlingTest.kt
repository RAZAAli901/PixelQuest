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
        // Since Day 27 Google's own 429 is "Gemini is busy" (ApiError 429), so it isn't mistaken for
        // PixelQuest's daily cap, which is what RateLimited means.
        assertEquals(GeminiResult.ApiError(429, AiErrorCopy.BUSY), result)
    }

    @Test
    fun safeGeminiCall_surfacesHttp429ApiException_asRateLimited() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiApiException(statusCode = 429, message = "Resource exhausted: rate limit exceeded")
        }

        assertEquals(GeminiResult.ApiError(429, AiErrorCopy.BUSY), result)
    }

    @Test
    fun safeGeminiCall_surfacesNetworkIOException_asNetworkError() = runBlocking {
        val result = safeGeminiCall {
            throw IOException("Failed to connect to generativelanguage.googleapis.com")
        }

        assertTrue("IOException must be mapped to NetworkError", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertNotNull(networkError.cause)
        assertEquals(AiErrorCopy.OFFLINE, networkError.message)
    }

    @Test
    fun safeGeminiCall_surfacesGeminiNetworkException_asNetworkError() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiNetworkException("SSL handshake failed", IOException("Connection reset"))
        }

        assertTrue("GeminiNetworkException must be mapped to NetworkError", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        // The user sees plain copy; the exception stays available as the cause.
        assertEquals(AiErrorCopy.OFFLINE, networkError.message)
        assertNotNull(networkError.cause)
    }

    @Test
    fun safeGeminiCall_surfacesTimeoutException_asNetworkError() = runBlocking {
        val result = safeGeminiCall(timeoutMs = 50L) {
            kotlinx.coroutines.delay(200L)
            "Done"
        }

        assertTrue("Timeout must be mapped to NetworkError", result is GeminiResult.NetworkError)
        val timeoutError = result as GeminiResult.NetworkError
        assertEquals(AiErrorCopy.TIMEOUT, timeoutError.message)
    }

    @Test
    fun safeGeminiCall_surfacesHttp500ApiError_asApiError() = runBlocking {
        val result = safeGeminiCall {
            throw GeminiApiException(statusCode = 500, message = "Internal Generative AI Server Error")
        }

        assertTrue("HTTP 500 must be mapped to ApiError", result is GeminiResult.ApiError)
        val apiError = result as GeminiResult.ApiError
        assertEquals(500, apiError.statusCode)
        assertEquals(AiErrorCopy.SERVER_TROUBLE, apiError.message)
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

        assertEquals("Pipeline must report Gemini as busy", GeminiResult.ApiError(429, AiErrorCopy.BUSY), result)
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

        assertTrue("Pipeline must return NetworkError on connectivity failure, was $result", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertEquals(AiErrorCopy.OFFLINE, networkError.message)
    }

    private fun createTestRepository(geminiClient: GeminiClient): HabitInsightRepositoryImpl {
        val streakRepo = com.pixelquest.app.testing.FakeStreakRepository(StreakEntity())

        val profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(UserProfileEntity(avatarId = "avatar_hero", username = "Hero", ))

        val taskRepo = com.pixelquest.app.testing.FakeTaskRepository(emptyList())

        val completionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository(emptyList())

        val settingsRepo = com.pixelquest.app.testing.FakeSettingsRepository(aiInsights = true)

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
