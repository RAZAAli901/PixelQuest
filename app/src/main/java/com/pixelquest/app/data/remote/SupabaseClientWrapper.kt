package com.pixelquest.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified wrapper for SupabaseClient providing configurable timeouts,
 * exponential backoff retries on transient network errors, and structured error mapping (Q045).
 */
interface SupabaseClientWrapper {
    val client: SupabaseClient
    val postgrest: Postgrest
    val auth: Auth

    suspend fun <T> execute(
        timeoutMs: Long = DEFAULT_SUPABASE_TIMEOUT_MS,
        retries: Int = DEFAULT_MAX_RETRIES,
        backoffBaseMs: Long = DEFAULT_BACKOFF_BASE_MS,
        block: suspend () -> T,
    ): SupabaseResult<T>

    companion object {
        const val DEFAULT_MAX_RETRIES = 2
        const val DEFAULT_BACKOFF_BASE_MS = 200L
    }
}

@Singleton
class DefaultSupabaseClientWrapper @Inject constructor(
    override val client: SupabaseClient,
) : SupabaseClientWrapper {

    override val postgrest: Postgrest get() = client.postgrest
    override val auth: Auth get() = client.auth

    override suspend fun <T> execute(
        timeoutMs: Long,
        retries: Int,
        backoffBaseMs: Long,
        block: suspend () -> T,
    ): SupabaseResult<T> {
        var attemptsLeft = retries
        var currentBackoff = backoffBaseMs

        while (true) {
            try {
                val data = withTimeout(timeoutMs) {
                    block()
                }
                return SupabaseResult.Success(data)
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    if (attemptsLeft > 0) {
                        attemptsLeft--
                        delay(currentBackoff)
                        currentBackoff *= 2
                        continue
                    }
                    return SupabaseResult.NetworkError(e, "Request timed out after ${timeoutMs / 1000}s. Please check your internet connection.")
                }
                throw e
            } catch (e: HttpRequestTimeoutException) {
                if (attemptsLeft > 0) {
                    attemptsLeft--
                    delay(currentBackoff)
                    currentBackoff *= 2
                    continue
                }
                return SupabaseResult.NetworkError(e, "Request timed out. Please check your internet connection.")
            } catch (e: IOException) {
                if (attemptsLeft > 0) {
                    attemptsLeft--
                    delay(currentBackoff)
                    currentBackoff *= 2
                    continue
                }
                return SupabaseResult.NetworkError(e, "Network error. Please verify your connection.")
            } catch (e: HttpRequestException) {
                if (attemptsLeft > 0) {
                    attemptsLeft--
                    delay(currentBackoff)
                    currentBackoff *= 2
                    continue
                }
                return SupabaseResult.NetworkError(e, "Network error communicating with Supabase.")
            } catch (e: RestException) {
                // Non-transient DB error: do not retry
                return SupabaseResult.ServerError(e.statusCode, e.error.ifBlank { "Database request failed." })
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                return if (msg.contains("auth", ignoreCase = true) || msg.contains("token", ignoreCase = true) || msg.contains("credential", ignoreCase = true)) {
                    SupabaseResult.AuthError(e, msg)
                } else {
                    SupabaseResult.UnknownError(e, msg)
                }
            }
        }
    }
}
