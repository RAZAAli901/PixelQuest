package com.pixelquest.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Header the gemini-proxy Edge Function counts calls by (see supabase/functions/gemini-proxy). */
const val AI_DEVICE_HEADER = "x-pixelquest-device"

/**
 * [GeminiClient] for release builds: sends the prompt to PixelQuest's gemini-proxy Supabase Edge
 * Function, which holds the Gemini API key, enforces daily limits and returns Gemini's response
 * unchanged. The app never has the key.
 */
class GeminiProxyClient(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val deviceIdProvider: () -> String,
    private val httpClient: HttpClient = HttpClient(Android)
) : GeminiClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateContent(
        prompt: String,
        systemInstruction: String?
    ): String = withContext(Dispatchers.IO) {
        val baseUrl = supabaseUrl.trim().trimEnd('/')
        if (!isConfigured(baseUrl)) {
            throw GeminiApiException(401, "The AI proxy is not configured (no Supabase project in this build).")
        }

        val body = buildJsonObject {
            put("prompt", prompt)
            if (!systemInstruction.isNullOrBlank()) put("systemInstruction", systemInstruction)
        }.toString()

        val response = try {
            httpClient.post("$baseUrl/functions/v1/gemini-proxy") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header("apikey", anonKey)
                header(AI_DEVICE_HEADER, deviceIdProvider())
                setBody(body)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw GeminiNetworkException("Failed to reach the AI proxy: ${e.message}", e)
        }

        val responseBody = response.bodyAsText()
        when (response.status) {
            HttpStatusCode.OK -> GeminiResponseParser.extractText(responseBody)
            // Gemini busy, or this device or the whole app has used today's calls.
            HttpStatusCode.TooManyRequests -> errorCode(responseBody).let { code ->
                throw GeminiRateLimitException("AI proxy limit reached ($code).", reason = code)
            }
            else -> throw GeminiApiException(response.status.value, "AI proxy error: ${errorCode(responseBody)}")
        }
    }

    /** The proxy's short error code (e.g. "device_daily_limit"); never Gemini's own error text. */
    private fun errorCode(body: String): String = try {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

    companion object {
        private const val PLACEHOLDER_URL = "https://placeholder-project.supabase.co"

        fun isConfigured(supabaseUrl: String): Boolean {
            val url = supabaseUrl.trim().trimEnd('/')
            return url.startsWith("https://") && url != PLACEHOLDER_URL
        }
    }
}
