package com.pixelquest.app.data.remote

import com.pixelquest.app.di.AiModule
import com.pixelquest.app.testing.FakeAiAccess
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The AI Coach is for signed-in players. Whichever client a build uses (the proxy, or the
 * developer's direct key), nothing is sent while nobody is signed in, and the refusal neither counts
 * toward the daily caps nor reads as an error the player could retry away.
 */
class AiNeedsAccountTest {

    private var sent = 0
    private val geminiOk = """{"candidates":[{"content":{"parts":[{"text":"{\"reply\":\"hi\"}"}]}}]}"""
    private val http = HttpClient(MockEngine {
        sent++
        respond(geminiOk, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    })

    private fun clientFor(viaProxy: Boolean, access: FakeAiAccess) = AiModule.createGeminiClient(
        viaProxy = viaProxy,
        httpClient = http,
        aiAccess = access,
        supabaseUrl = "https://abcd1234.supabase.co",
        anonKey = "sb_publishable_test",
        geminiApiKey = "developer-key"
    )

    @Test
    fun signedOut_neitherClientSendsAnything() = runBlocking {
        for (viaProxy in listOf(true, false)) {
            val result = safeGeminiCall { clientFor(viaProxy, FakeAiAccess(token = null)).generateContent("hi") }

            assertTrue("viaProxy=$viaProxy", result is GeminiResult.SignInRequired)
            assertFalse("Doesn't count toward the caps", result.reachedGemini())
        }
        assertEquals(0, sent)
    }

    @Test
    fun signedIn_bothClientsWork() = runBlocking {
        for (viaProxy in listOf(true, false)) {
            val result = safeGeminiCall { clientFor(viaProxy, FakeAiAccess()).generateContent("hi") }
            assertEquals("viaProxy=$viaProxy", GeminiResult.Success("{\"reply\":\"hi\"}"), result)
        }
        assertEquals(2, sent)
    }

    @Test
    fun signingOut_stopsTheNextCall() = runBlocking {
        val access = FakeAiAccess()
        val client = clientFor(viaProxy = true, access = access)
        assertTrue(safeGeminiCall { client.generateContent("hi") }.isSuccess)

        access.token = null

        assertTrue(safeGeminiCall { client.generateContent("hi") } is GeminiResult.SignInRequired)
        assertEquals(1, sent)
    }
}
