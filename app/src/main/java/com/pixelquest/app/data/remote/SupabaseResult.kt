package com.pixelquest.app.data.remote

import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException

/**
 * Sealed result type wrapping Supabase remote operations with typed error cases.
 */
sealed class SupabaseResult<out T> {
    data class Success<out T>(val data: T) : SupabaseResult<T>()
    data class NetworkError(
        val exception: Throwable,
        val message: String = "Network connection unavailable."
    ) : SupabaseResult<Nothing>()

    data class AuthError(
        val exception: Throwable,
        val message: String = "Authentication failed."
    ) : SupabaseResult<Nothing>()

    data class ServerError(
        val code: Int? = null,
        val message: String = "Supabase server error."
    ) : SupabaseResult<Nothing>()

    data class UnknownError(
        val exception: Throwable,
        val message: String = "An unexpected error occurred."
    ) : SupabaseResult<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

const val DEFAULT_SUPABASE_TIMEOUT_MS = 15_000L

/**
 * Executes a suspending Supabase operation safely with a timeout,
 * catching network, timeout, rest, auth, and unexpected exceptions.
 */
suspend fun <T> safeSupabaseCall(
    timeoutMs: Long = DEFAULT_SUPABASE_TIMEOUT_MS,
    block: suspend () -> T
): SupabaseResult<T> {
    return try {
        val data = kotlinx.coroutines.withTimeout(timeoutMs) {
            block()
        }
        SupabaseResult.Success(data)
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        SupabaseResult.NetworkError(e, "Request timed out after ${timeoutMs / 1000}s. Please check your internet connection.")
    } catch (e: HttpRequestTimeoutException) {
        SupabaseResult.NetworkError(e, "Request timed out. Please check your internet connection.")
    } catch (e: IOException) {
        SupabaseResult.NetworkError(e, "Network error. Please verify your connection.")
    } catch (e: io.github.jan.supabase.exceptions.HttpRequestException) {
        SupabaseResult.NetworkError(e, "Network error communicating with Supabase.")
    } catch (e: io.github.jan.supabase.exceptions.RestException) {
        SupabaseResult.ServerError(e.statusCode, e.error.ifBlank { "Database request failed." })
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        val msg = e.message ?: "Unknown error"
        if (msg.contains("auth", ignoreCase = true) || msg.contains("token", ignoreCase = true) || msg.contains("credential", ignoreCase = true)) {
            SupabaseResult.AuthError(e, msg)
        } else {
            SupabaseResult.UnknownError(e, msg)
        }
    }
}
