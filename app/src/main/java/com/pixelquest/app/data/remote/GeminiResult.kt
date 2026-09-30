package com.pixelquest.app.data.remote

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
        GeminiResult.NetworkError(e, "AI request timed out after ${timeoutMs / 1000}s. Please check your internet connection.")
    } catch (e: HttpRequestTimeoutException) {
        GeminiResult.NetworkError(e, "AI request timed out. Please verify your connection.")
    } catch (e: IOException) {
        GeminiResult.NetworkError(e, "Network error contacting Gemini. Please verify your connection.")
    } catch (e: GeminiRateLimitException) {
        GeminiResult.RateLimited(e.retryAfterSeconds, e.message ?: "Rate limit reached.")
    } catch (e: GeminiApiException) {
        if (e.statusCode == 429) {
            GeminiResult.RateLimited(null, "Rate limit reached (429).")
        } else {
            GeminiResult.ApiError(e.statusCode, e.message ?: "API error")
        }
    } catch (e: GeminiNetworkException) {
        GeminiResult.NetworkError(e.cause ?: e, e.message ?: "Network failure")
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        GeminiResult.ApiError(null, e.message ?: "An unexpected error occurred during AI generation.")
    }
}
