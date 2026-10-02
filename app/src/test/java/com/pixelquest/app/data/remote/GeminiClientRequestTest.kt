package com.pixelquest.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The Gemini API key must travel in the x-goog-api-key header and never appear in the request URL.
 */
class GeminiClientRequestTest {

    private val testKey = "test-key-123"
    private val okBody = """{"candidates":[{"content":{"parts":[{"text":"{\"reply\":\"hello\"}"}]}}]}"""

    private fun clientReturning(status: HttpStatusCode, body: String, requests: MutableList<HttpRequestData>): GeminiClientImpl {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return GeminiClientImpl(apiKeyProvider = { testKey }, httpClient = HttpClient(engine))
    }

    @Test
    fun apiKey_isSentInHeader_notInUrl() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val client = clientReturning(HttpStatusCode.OK, okBody, requests)

        client.generateContent("Say hello")

        val request = requests.single()
        assertEquals(testKey, request.headers[API_KEY_HEADER])
        assertFalse("Key must not be in the URL", request.url.toString().contains(testKey))
        assertFalse("No key query parameter", request.url.parameters.contains("key"))
        assertTrue(request.url.toString().endsWith("models/$GEMINI_MODEL_ID:generateContent"))
    }

    @Test
    fun successfulResponse_returnsCandidateText() = runBlocking {
        val client = clientReturning(HttpStatusCode.OK, okBody, mutableListOf())

        assertEquals("{\"reply\":\"hello\"}", client.generateContent("Say hello"))
    }

    @Test
    fun rateLimit_throwsRateLimitException() = runBlocking {
        val client = clientReturning(HttpStatusCode.TooManyRequests, "{}", mutableListOf())
        try {
            client.generateContent("Say hello")
            fail("Expected GeminiRateLimitException")
        } catch (e: GeminiRateLimitException) {
            assertFalse(e.message.orEmpty().contains(testKey))
        }
    }

    @Test
    fun apiError_messageNeverContainsTheKey() = runBlocking {
        val client = clientReturning(HttpStatusCode.Forbidden, """{"error":{"message":"API key not valid"}}""", mutableListOf())
        try {
            client.generateContent("Say hello")
            fail("Expected GeminiApiException")
        } catch (e: GeminiApiException) {
            assertEquals(403, e.statusCode)
            assertFalse(e.message.orEmpty().contains(testKey))
        }
    }
}
