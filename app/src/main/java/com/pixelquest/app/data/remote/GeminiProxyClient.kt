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

/**
 * [GeminiClient] for release builds: sends the prompt to PixelQuest's gemini-proxy Supabase Edge
 * Function, which holds the Gemini API key, checks that the caller is a signed-in account, enforces
 * daily limits and returns Gemini's response unchanged. The app never has the key.
 */
class GeminiProxyClient(
    private val supabaseUrl: String,
    private val anonKey: String,
    /** The signed-in account's access token, or null when nobody is signed in (see AiAccess). */
    private val accessTokenProvider: suspend () -> String?,
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

        // The proxy serves signed-in accounts only, so without one nothing is sent.
        val accessToken = accessTokenProvider() ?: throw GeminiSignInRequiredException()

        val body = buildJsonObject {
            put("prompt", prompt)
            if (!systemInstruction.isNullOrBlank()) put("systemInstruction", systemInstruction)
        }.toString()

        val response = try {
            httpClient.post("$baseUrl/functions/v1/gemini-proxy") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $accessToken")
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
            // The account's session ended or was revoked: the player has to sign in again.
            HttpStatusCode.Unauthorized -> if (errorCode(responseBody) == "sign_in_required") {
                throw GeminiSignInRequiredException()
            } else {
                throw GeminiApiException(401, "AI proxy error: ${errorCode(responseBody)}")
            }
            // Gemini busy, or this account or the whole app has used today's calls.
            HttpStatusCode.TooManyRequests -> errorCode(responseBody).let { code ->
                throw GeminiRateLimitException("AI proxy limit reached ($code).", reason = code)
            }
            else -> throw GeminiApiException(response.status.value, "AI proxy error: ${errorCode(responseBody)}")
        }
    }

    /** The proxy's short error code (e.g. "account_daily_limit"); never Gemini's own error text. */
    private fun errorCode(body: String): String = try {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

    companion object {
        /** The proxy needs only the project URL (the function doesn't check the anon key). */
        fun isConfigured(supabaseUrl: String): Boolean = com.pixelquest.app.domain.CloudAvailability.isRealProjectUrl(supabaseUrl)
    }
}
