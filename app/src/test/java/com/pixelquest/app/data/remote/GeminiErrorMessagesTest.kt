package com.pixelquest.app.data.remote

import com.pixelquest.app.domain.ai.AiErrorCopy
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

/**
 * Every Gemini failure reaches the AI Coach as one plain sentence; raw HTTP bodies and exception
 * text never do.
 */
class GeminiErrorMessagesTest {

    private fun client(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        GeminiClientImpl(apiKeyProvider = { "test-key" }, httpClient = HttpClient(MockEngine(handler)))

    private fun json(status: HttpStatusCode, body: String): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }

    private fun messageOf(result: GeminiResult<String>): String = when (result) {
        is GeminiResult.ApiError -> result.message
        is GeminiResult.NetworkError -> result.message
        is GeminiResult.RateLimited -> result.message
        is GeminiResult.MalformedResponse -> result.message
        is GeminiResult.Disabled -> result.message
        is GeminiResult.SignInRequired -> result.message
        is GeminiResult.Success -> error("Expected a failure")
    }

    private fun assertPlain(message: String) {
        assertFalse("No raw JSON in '$message'", message.contains("{"))
        assertFalse("No status codes in '$message'", Regex("\\(\\d{3}\\)").containsMatchIn(message))
        assertFalse("No API key in '$message'", message.contains("test-key"))
    }

    @Test
    fun rejectedKey_showsUnavailable() = runBlocking {
        val body = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.","status":"INVALID_ARGUMENT"}}"""
        val result = safeGeminiCall { client(json(HttpStatusCode.BadRequest, body)).generateContent("hi") }

        assertTrue(result is GeminiResult.ApiError)
        assertEquals(AiErrorCopy.UNAVAILABLE, messageOf(result))
        assertPlain(messageOf(result))
    }

    @Test
    fun aBuildWithoutAKey_saysTheCoachIsNotSetUp() = runBlocking {
        val noKey = GeminiClientImpl(apiKeyProvider = { "placeholder-gemini-key" }, httpClient = HttpClient(MockEngine { error("must not send") }))
        val noProject = GeminiProxyClient("https://placeholder-project.supabase.co", "placeholder-anon-key", { "account-token" }, HttpClient(MockEngine { error("must not send") }))

        assertEquals(AiErrorCopy.NOT_CONFIGURED, messageOf(safeGeminiCall { noKey.generateContent("hi") }))
        assertEquals(AiErrorCopy.NOT_CONFIGURED, messageOf(safeGeminiCall { noProject.generateContent("hi") }))
    }

    @Test
    fun theProxysDailyLimits_sayTryTomorrow_butGeminiBusyThroughTheProxySaysMinutes() = runBlocking {
        fun proxy(code: String) = GeminiProxyClient(
            "https://abcd.supabase.co", "anon", { "account-token" },
            HttpClient(MockEngine { respond("""{"error":"$code"}""", HttpStatusCode.TooManyRequests) })
        )

        assertEquals(AiErrorCopy.DAILY_LIMIT, messageOf(safeGeminiCall { proxy("account_daily_limit").generateContent("hi") }))
        assertEquals(AiErrorCopy.DAILY_LIMIT, messageOf(safeGeminiCall { proxy("global_daily_limit").generateContent("hi") }))
        assertEquals(AiErrorCopy.BUSY, messageOf(safeGeminiCall { proxy("upstream_busy").generateContent("hi") }))
    }

    @Test
    fun forbidden_showsUnavailable() = runBlocking {
        val result = safeGeminiCall { client(json(HttpStatusCode.Forbidden, """{"error":{"status":"PERMISSION_DENIED"}}""")).generateContent("hi") }

        assertEquals(AiErrorCopy.UNAVAILABLE, messageOf(result))
    }

    @Test
    fun googleQuota_showsBusy_notTheDailyCap() = runBlocking {
        val result = safeGeminiCall { client(json(HttpStatusCode.TooManyRequests, "{}")).generateContent("hi") }

        assertTrue("A Gemini 429 must not look like PixelQuest's daily cap", result is GeminiResult.ApiError)
        assertEquals(AiErrorCopy.BUSY, messageOf(result))
        assertFalse(messageOf(result).contains("limit", ignoreCase = true))
    }

    @Test
    fun serverError_showsServerTrouble() = runBlocking {
        val result = safeGeminiCall { client(json(HttpStatusCode.ServiceUnavailable, """{"error":{"status":"UNAVAILABLE"}}""")).generateContent("hi") }

        assertEquals(AiErrorCopy.SERVER_TROUBLE, messageOf(result))
        assertPlain(messageOf(result))
    }

    @Test
    fun noCandidates_showsUnreadable() = runBlocking {
        val result = safeGeminiCall { client(json(HttpStatusCode.OK, """{"promptFeedback":{"blockReason":"SAFETY"}}""")).generateContent("hi") }

        assertEquals(AiErrorCopy.UNREADABLE, messageOf(result))
        assertPlain(messageOf(result))
    }

    @Test
    fun offline_showsOffline() = runBlocking {
        val result = safeGeminiCall { client { throw UnknownHostException("generativelanguage.googleapis.com") }.generateContent("hi") }

        assertTrue(result is GeminiResult.NetworkError)
        assertEquals(AiErrorCopy.OFFLINE, messageOf(result))
        assertFalse(messageOf(result).contains("googleapis"))
    }

    @Test
    fun slowAnswer_showsTimeout() = runBlocking {
        val slow = client { delay(2_000); respond("{}", HttpStatusCode.OK) }
        val result = safeGeminiCall(timeoutMs = 100) { slow.generateContent("hi") }

        assertTrue(result is GeminiResult.NetworkError)
        assertEquals(AiErrorCopy.TIMEOUT, messageOf(result))
    }

    @Test
    fun statusTable_coversEveryCategory() {
        assertEquals(AiErrorCopy.NOT_CONFIGURED, AiErrorCopy.forStatus(401))
        assertEquals(AiErrorCopy.UNAVAILABLE, AiErrorCopy.forStatus(404))
        assertEquals(AiErrorCopy.BUSY, AiErrorCopy.forStatus(429))
        assertEquals(AiErrorCopy.SERVER_TROUBLE, AiErrorCopy.forStatus(500))
        assertEquals(AiErrorCopy.UNREADABLE, AiErrorCopy.forStatus(200))
        assertEquals(AiErrorCopy.UNKNOWN, AiErrorCopy.forStatus(null))
    }
}
