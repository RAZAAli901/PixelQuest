package com.pixelquest.app.data.remote

import com.pixelquest.app.domain.ai.AiErrorCopy
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

/**
 * Sealed result type wrapping Gemini remote AI operations with typed error cases,
 * consistent with the Day 13 [SupabaseResult] architectural pattern.
 */
sealed class GeminiResult<out T> {
    data class Success<out T>(val data: T) : GeminiResult<T>()

    data class RateLimited(
        val retryAfterSeconds: Long? = null,
        val message: String = "Gemini rate limit exceeded. Please wait before generating more insights."
    ) : GeminiResult<Nothing>()

    data class NetworkError(
        val cause: Throwable,
        val message: String = "Network connection unavailable. Please check your internet connection."
    ) : GeminiResult<Nothing>()

    data class ApiError(
        val statusCode: Int? = null,
        val message: String = "Gemini API request failed."
    ) : GeminiResult<Nothing>()

    data class Disabled(
        val message: String = "AI Habit Insights are currently disabled in Settings."
    ) : GeminiResult<Nothing>()

    data class MalformedResponse(
        val cause: Throwable,
        val rawResponse: String? = null,
        val message: String = "Failed to parse structured AI insight."
    ) : GeminiResult<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

const val DEFAULT_GEMINI_TIMEOUT_MS = 25_000L

/**
 * Executes a suspending Gemini API operation safely with a timeout,
 * catching network, timeout, rate-limit, and API exceptions.
 */
suspend fun <T> safeGeminiCall(
    timeoutMs: Long = DEFAULT_GEMINI_TIMEOUT_MS,
    block: suspend () -> T
): GeminiResult<T> {
    return try {
        val data = kotlinx.coroutines.withTimeout(timeoutMs) {
            block()
        }
        GeminiResult.Success(data)
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        GeminiResult.NetworkError(e, AiErrorCopy.TIMEOUT)
    } catch (e: HttpRequestTimeoutException) {
        GeminiResult.NetworkError(e, AiErrorCopy.TIMEOUT)
    } catch (e: IOException) {
        GeminiResult.NetworkError(e, AiErrorCopy.OFFLINE)
    } catch (e: GeminiRateLimitException) {
        // Google's own quota (HTTP 429), not PixelQuest's daily cap: the user can retry shortly.
        GeminiResult.ApiError(429, AiErrorCopy.BUSY)
    } catch (e: GeminiApiException) {
        GeminiResult.ApiError(e.statusCode, AiErrorCopy.forStatus(e.statusCode))
    } catch (e: GeminiNetworkException) {
        val cause = e.cause ?: e
        val isTimeout = cause is HttpRequestTimeoutException || cause is java.net.SocketTimeoutException
        GeminiResult.NetworkError(cause, if (isTimeout) AiErrorCopy.TIMEOUT else AiErrorCopy.OFFLINE)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        GeminiResult.ApiError(null, AiErrorCopy.UNKNOWN)
    }
}
