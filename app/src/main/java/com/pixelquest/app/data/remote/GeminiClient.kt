package com.pixelquest.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Gemini model used for all PixelQuest generation calls. gemini-1.5-flash was retired by Google;
 * confirm this id against the models list for your API key when upgrading.
 */
const val GEMINI_MODEL_ID = "gemini-2.5-flash"

/** Request header that carries the Gemini API key. */
const val API_KEY_HEADER = "x-goog-api-key"

/**
 * Interface representing a remote Google Gemini generative AI client.
 */
interface GeminiClient {
    /**
     * Sends a generation request to the Gemini API and returns the raw string response content.
     *
     * @param prompt The user/habit context prompt.
     * @param systemInstruction Optional system-level instructions directing persona and format.
     * @return Generated text string from the Gemini model.
     * @throws GeminiException on API, HTTP, or parsing failure.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null
    ): String
}

/**
 * Exception thrown when the Gemini API returns an error or unexpected response.
 */
open class GeminiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class GeminiRateLimitException(message: String, val retryAfterSeconds: Long? = null) : GeminiException(message)
class GeminiApiException(val statusCode: Int, message: String) : GeminiException("Gemini API error ($statusCode): $message")
class GeminiNetworkException(message: String, cause: Throwable) : GeminiException(message, cause)

/**
 * Default implementation of [GeminiClient] communicating directly with the Generative Language API
 * via Ktor over REST.
 */
class GeminiClientImpl(
    private val apiKeyProvider: () -> String,
    private val httpClient: HttpClient = HttpClient(Android),
    private val model: String = GEMINI_MODEL_ID
) : GeminiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun generateContent(
        prompt: String,
        systemInstruction: String?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank() || apiKey == "placeholder-gemini-key") {
            throw GeminiApiException(401, "Gemini API key is not configured.")
        }

        // The key travels in the x-goog-api-key header, never in the URL, so it stays out of
        // request logs and exception messages that include the URL.
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val requestPayload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    }
                })
            }

            if (!systemInstruction.isNullOrBlank()) {
                putJsonObject("system_instruction") {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", systemInstruction)
                        })
                    }
                }
            }

            putJsonObject("generationConfig") {
                put("temperature", 0.7)
                put("maxOutputTokens", 800)
                put("responseMimeType", "application/json")
            }
        }.toString()

        val response: HttpResponse = try {
            httpClient.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(API_KEY_HEADER, apiKey)
                setBody(requestPayload)
            }
        } catch (e: Exception) {
            throw GeminiNetworkException("Failed to connect to Gemini API: ${e.message}", e)
        }

        val statusCode = response.status.value
        val responseBody = response.bodyAsText()

        when (response.status) {
            HttpStatusCode.OK -> {
                extractTextFromResponse(responseBody)
            }
            HttpStatusCode.TooManyRequests -> {
                throw GeminiRateLimitException("Gemini rate limit exceeded (HTTP 429).")
            }
            else -> {
                throw GeminiApiException(statusCode, responseBody)
            }
        }
    }

    private fun extractTextFromResponse(responseBody: String): String {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val candidates = root["candidates"]?.jsonArray
            if (candidates.isNullOrEmpty()) {
                throw GeminiApiException(200, "No candidates returned: $responseBody")
            }

            val firstCandidate = candidates[0].jsonObject
            val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
            val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

            text ?: throw GeminiApiException(200, "Empty text in candidate: $responseBody")
        } catch (e: GeminiException) {
            throw e
        } catch (e: Exception) {
            throw GeminiApiException(200, "Failed to parse Gemini response: ${e.message}")
        }
    }
}
