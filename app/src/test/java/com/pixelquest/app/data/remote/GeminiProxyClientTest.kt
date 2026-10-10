package com.pixelquest.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Release builds reach Gemini only through the gemini-proxy Edge Function, without the key, and only
 * for a signed-in account: the request carries the account's access token, which the proxy checks.
 */
class GeminiProxyClientTest {

    private val supabaseUrl = "https://abcd1234.supabase.co"
    private val anonKey = "sb_publishable_test"
    private val accessToken = "account-access-token"
    private val geminiOk = """{"candidates":[{"content":{"parts":[{"text":"{\"reply\":\"hello\"}"}]}}]}"""

    private fun client(
        status: HttpStatusCode,
        body: String,
        requests: MutableList<HttpRequestData> = mutableListOf(),
        url: String = supabaseUrl,
        token: String? = accessToken
    ): GeminiProxyClient {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return GeminiProxyClient(url, anonKey, { token }, HttpClient(engine))
    }

    @Test
    fun request_goesToTheEdgeFunction_withTheAccountsToken_andNoGeminiKey() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()

        val text = client(HttpStatusCode.OK, geminiOk, requests).generateContent("How am I doing?", "Reply in JSON")

        assertEquals("{\"reply\":\"hello\"}", text)
        val request = requests.single()
        assertEquals("$supabaseUrl/functions/v1/gemini-proxy", request.url.toString())
        assertEquals("Bearer $accessToken", request.headers[HttpHeaders.Authorization])
        assertNull("No install id any more", request.headers["x-pixelquest-device"])
        assertEquals(anonKey, request.headers["apikey"])
        assertNull("No Gemini key header", request.headers[API_KEY_HEADER])

        val sent = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals("How am I doing?", sent["prompt"]!!.jsonPrimitive.content)
        assertEquals("Reply in JSON", sent["systemInstruction"]!!.jsonPrimitive.content)
        assertEquals(setOf("prompt", "systemInstruction"), sent.keys)
    }

    @Test
    fun aTrailingSlashInTheProjectUrl_isFine() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        client(HttpStatusCode.OK, geminiOk, requests, url = "$supabaseUrl/").generateContent("hi")

        assertEquals("$supabaseUrl/functions/v1/gemini-proxy", requests.single().url.toString())
    }

    @Test
    fun withoutASupabaseProject_nothingIsSent() = runBlocking {
        for (url in listOf("https://placeholder-project.supabase.co", "", "http://insecure.example")) {
            val requests = mutableListOf<HttpRequestData>()
            try {
                client(HttpStatusCode.OK, geminiOk, requests, url = url).generateContent("hi")
                fail("Expected GeminiApiException for '$url'")
            } catch (e: GeminiApiException) {
                assertEquals(401, e.statusCode)
            }
            assertTrue(requests.isEmpty())
        }
    }

    @Test
    fun signedOut_nothingIsSent() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        try {
            client(HttpStatusCode.OK, geminiOk, requests, token = null).generateContent("hi")
            fail("Expected GeminiSignInRequiredException")
        } catch (e: GeminiSignInRequiredException) {
            assertTrue(requests.isEmpty())
        }
    }

    @Test
    fun aTokenTheProxyRefuses_meansSignInAgain() = runBlocking {
        try {
            client(HttpStatusCode.Unauthorized, """{"error":"sign_in_required"}""").generateContent("hi")
            fail("Expected GeminiSignInRequiredException")
        } catch (e: GeminiSignInRequiredException) {
            // The AI Coach then asks the player to sign in again.
        }
    }

    @Test
    fun dailyLimits_areRateLimits() = runBlocking {
        for (code in listOf("account_daily_limit", "global_daily_limit", "upstream_busy")) {
            try {
                client(HttpStatusCode.TooManyRequests, """{"error":"$code"}""").generateContent("hi")
                fail("Expected GeminiRateLimitException")
            } catch (e: GeminiRateLimitException) {
                assertTrue(e.message!!.contains(code))
            }
        }
    }

    @Test
    fun otherProxyErrors_keepTheirStatus_andOnlyTheShortCode() = runBlocking {
        try {
            client(HttpStatusCode.BadGateway, """{"error":"upstream_error","status":400}""").generateContent("hi")
            fail("Expected GeminiApiException")
        } catch (e: GeminiApiException) {
            assertEquals(502, e.statusCode)
            assertTrue(e.message!!.contains("upstream_error"))
        }
        try {
            client(HttpStatusCode.ServiceUnavailable, "<html>gateway</html>").generateContent("hi")
            fail("Expected GeminiApiException")
        } catch (e: GeminiApiException) {
            assertEquals(503, e.statusCode)
            assertFalse(e.message!!.contains("<html>"))
        }
    }

    @Test
    fun anAnswerWithoutText_isReportedLikeADirectCall() = runBlocking {
        val noText = """{"candidates":[{"content":{"role":"model"},"finishReason":"MAX_TOKENS"}]}"""
        try {
            client(HttpStatusCode.OK, noText).generateContent("hi")
            fail("Expected GeminiApiException")
        } catch (e: GeminiApiException) {
            assertEquals(200, e.statusCode)
            assertTrue(e.message!!.contains("MAX_TOKENS"))
        }
    }

    @Test
    fun networkFailure_isANetworkError() = runBlocking {
        val engine = MockEngine { throw java.io.IOException("no route to host") }
        try {
            GeminiProxyClient(supabaseUrl, anonKey, { accessToken }, HttpClient(engine)).generateContent("hi")
            fail("Expected GeminiNetworkException")
        } catch (e: GeminiNetworkException) {
            // Ktor may wrap the IOException; what matters is that it's reported as a network error.
            assertTrue(e.message!!.startsWith("Failed to reach the AI proxy"))
        }
    }

    @Test
    fun proxyFailuresBeforeGemini_dontUseUpTheDaysCalls() = runBlocking {
        val notReached = listOf(
            HttpStatusCode.ServiceUnavailable to "usage_unavailable",
            HttpStatusCode.ServiceUnavailable to "auth_unavailable",
            HttpStatusCode.ServiceUnavailable to "not_configured",
            HttpStatusCode.BadGateway to "upstream_unreachable"
        )
        for ((status, code) in notReached) {
            val result = safeGeminiCall { client(status, """{"error":"$code"}""").generateContent("hi") }
            assertTrue(code, result is GeminiResult.ApiError)
            assertFalse("$code didn't reach Gemini", result.reachedGemini())
        }
        // Gemini answered with an error: that one counts.
        val geminiError = safeGeminiCall { client(HttpStatusCode.BadGateway, """{"error":"upstream_error","status":500}""").generateContent("hi") }
        assertTrue(geminiError.reachedGemini())
    }
}
