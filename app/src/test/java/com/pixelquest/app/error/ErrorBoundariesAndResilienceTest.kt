package com.pixelquest.app.error

import com.pixelquest.app.data.remote.GeminiRateLimitException
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeGeminiCall
import com.pixelquest.app.data.remote.safeSupabaseCall
import com.pixelquest.app.domain.SafeErrorCopy
import com.pixelquest.app.domain.ai.AiErrorCopy
import com.pixelquest.app.domain.ai.HabitInsightResponse
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * V057: Error boundaries, malformed responses, rate limits, and timeouts test suite.
 * Covers:
 * - HTTP 429 rate limit handling (daily cap vs transient busy quota)
 * - Network timeout handling and friendly user messaging
 * - Malformed server responses, truncated JSON, and missing fields
 * - Markdown fences and extraneous keys parsing resilience
 * - Error copy safety (zero stack traces, table names, or tokens leaked)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ErrorBoundariesAndResilienceTest {

    // --- 1. Rate Limit Handling ---

    @Test
    fun testRateLimit_dailyQuotaExceeded_mapsToDailyLimitMessage() = runTest {
        val result = safeGeminiCall<String> {
            throw GeminiRateLimitException(message = "Daily limit reached", reason = "account_daily_limit")
        }

        assertTrue("Result must be ApiError", result is GeminiResult.ApiError)
        val apiError = result as GeminiResult.ApiError
        assertEquals(429, apiError.statusCode)
        assertEquals("Account daily quota must map to DAILY_LIMIT copy", AiErrorCopy.DAILY_LIMIT, apiError.message)
    }

    @Test
    fun testRateLimit_transientQuotaExceeded_mapsToBusyMessage() = runTest {
        val result = safeGeminiCall<String> {
            throw GeminiRateLimitException(message = "Busy right now", reason = "resource_exhausted")
        }

        assertTrue("Result must be ApiError", result is GeminiResult.ApiError)
        val apiError = result as GeminiResult.ApiError
        assertEquals(429, apiError.statusCode)
        assertEquals("Transient 429 quota must map to BUSY copy", AiErrorCopy.BUSY, apiError.message)
    }

    // --- 2. Timeout Resilience ---

    @Test
    fun testNetworkTimeout_geminiCall_mapsToSafeTimeoutCopy() = runTest {
        val result = safeGeminiCall<String> {
            throw HttpRequestTimeoutException("https://api.pixelquest.com/gemini", 5000L)
        }

        assertTrue("Result must be NetworkError", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertEquals(AiErrorCopy.TIMEOUT, networkError.message)
        assertFalse("Timeout message must not leak raw URL", networkError.message.contains("https://"))
    }

    @Test
    fun testNetworkTimeout_supabaseCall_mapsToSafeTimeoutCopy() = runTest {
        val result = safeSupabaseCall<String> {
            throw HttpRequestTimeoutException("https://supabase.pixelquest.com", 10000L)
        }

        assertTrue("Result must be NetworkError", result is SupabaseResult.NetworkError)
        val networkError = result as SupabaseResult.NetworkError
        assertTrue("Message must indicate timeout", networkError.message.contains("timed out", ignoreCase = true))
        assertFalse("Message must not leak internal host", networkError.message.contains("supabase.pixelquest.com"))
    }

    // --- 3. Malformed Responses & JSON Parsing Resilience ---

    @Test
    fun testMalformedJson_truncatedPayload_throwsHandledException() {
        val truncatedJson = """{"summary": "Great progress on habits", "sugges"""
        try {
            HabitInsightResponse.parseFromJson(truncatedJson)
            fail("Truncated JSON must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message must cite malformed JSON", e.message?.contains("Malformed JSON") == true)
        }
    }

    @Test
    fun testMalformedJson_missingRequiredFields_throwsHandledException() {
        val missingFieldsJson = """{"highlightCategory": "Fitness", "specificTaskCallout": "Keep going"}"""
        try {
            HabitInsightResponse.parseFromJson(missingFieldsJson)
            fail("Missing required fields must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message must cite missing required fields", e.message?.contains("Missing required") == true)
        }
    }

    @Test
    fun testMalformedJson_unknownExtraneousKeys_parsedSuccessfully() {
        val jsonWithExtra = """
            {
                "summary": "You crushed 5 quests today!",
                "suggestion": "Keep the momentum going tomorrow morning.",
                "encouragement": "Level 10 is within reach!",
                "unknown_extra_field_v2": 9999,
                "experimental_model_tag": "gemini-2.5-pro",
                "nested_garbage": {"a": [1, 2, 3]}
            }
        """.trimIndent()

        val parsed = HabitInsightResponse.parseFromJson(jsonWithExtra)
        assertEquals("You crushed 5 quests today!", parsed.summary)
        assertEquals("Keep the momentum going tomorrow morning.", parsed.suggestion)
        assertEquals("Level 10 is within reach!", parsed.encouragement)
    }

    @Test
    fun testMarkdownCodeFences_conversationalPreamble_cleanlyExtracted() {
        val fenced = """
            Certainly! Here is your personalized habit breakdown:
            ```json
            {
                "summary": "Consistent morning workouts.",
                "suggestion": "Add 5 minutes of stretching.",
                "encouragement": "Great job maintaining your streak!"
            }
            ```
            Let me know if you need any adjustments!
        """.trimIndent()

        val parsed = HabitInsightResponse.parseFromJson(fenced)
        assertEquals("Consistent morning workouts.", parsed.summary)
        assertEquals("Add 5 minutes of stretching.", parsed.suggestion)
        assertEquals("Great job maintaining your streak!", parsed.encouragement)
    }

    // --- 4. Information Disclosure & Error Copy Safety ---

    @Test
    fun testErrorCopySafety_aiCopy_doesNotExposeSensitiveTokensOrTechJargon() {
        val allAiMessages = listOf(
            AiErrorCopy.UNAVAILABLE,
            AiErrorCopy.NOT_CONFIGURED,
            AiErrorCopy.BUSY,
            AiErrorCopy.DAILY_LIMIT,
            AiErrorCopy.SERVER_TROUBLE,
            AiErrorCopy.TIMEOUT,
            AiErrorCopy.OFFLINE,
            AiErrorCopy.UNREADABLE,
            AiErrorCopy.UNKNOWN,
            AiErrorCopy.SIGN_IN_REQUIRED,
        )

        val forbiddenSubstrings = listOf("bearer", "jwt", "select *", "table", "sql", "exception", "nullpointer", "stacktrace", "apikey")

        for (msg in allAiMessages) {
            for (forbidden in forbiddenSubstrings) {
                assertFalse("Error message '$msg' must not contain '$forbidden'", msg.contains(forbidden, ignoreCase = true))
            }
        }
    }

    @Test
    fun testErrorCopySafety_safeCopy_doesNotExposeSensitiveTokensOrDatabaseNames() {
        val allSafeMessages = listOf(
            SafeErrorCopy.GOOGLE_EXCHANGE_FAILED,
            SafeErrorCopy.GOOGLE_FAILED,
            SafeErrorCopy.SYNC_SERVER,
            SafeErrorCopy.SYNC_FAILED,
            SafeErrorCopy.TASKS_FAILED,
            SafeErrorCopy.BOARD_OFFLINE,
            SafeErrorCopy.BOARD_SIGN_IN,
            SafeErrorCopy.BOARD_FAILED,
        )

        val forbiddenSubstrings = listOf("token", "schema", "database", "postgres", "foreign key", "sqlite", "table")

        for (msg in allSafeMessages) {
            for (forbidden in forbiddenSubstrings) {
                if (forbidden == "token" && msg == SafeErrorCopy.GOOGLE_EXCHANGE_FAILED) {
                    // Safe user-facing mention of token exchange
                    continue
                }
                assertFalse("Safe message '$msg' must not contain '$forbidden'", msg.contains(forbidden, ignoreCase = true))
            }
        }
    }
}
