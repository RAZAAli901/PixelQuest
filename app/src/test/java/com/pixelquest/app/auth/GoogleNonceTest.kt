package com.pixelquest.app.auth

import android.app.Application
import android.content.Context
import com.pixelquest.app.data.remote.SupabaseResult
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sign in with Google sends Google the SHA-256 of a nonce, so the ID token carries it, and Supabase
 * needs the raw nonce to check it. The raw value used to be dropped, and Supabase refuses a token
 * with a nonce when none is passed, so Google sign-in could never have worked against a real project.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GoogleNonceTest {

    @Test
    fun theRawNonce_goesFromGoogleSignIn_toTheTokenExchange() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val context: Context = io.mockk.mockk(relaxed = true)
            val google = FakeGoogleAuthManager(context).apply {
                signInResult = GoogleAuthResult.Success("id-token", "hero@pixelquest.test", "Hero", rawNonce = "raw-nonce-1")
            }
            var passedNonce: String? = null
            val repo = object : AuthRepository by FakeAuthRepository() {
                override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?): SupabaseResult<AuthUser> {
                    passedNonce = rawNonce
                    return SupabaseResult.Success(AuthUser("user-1", "hero@pixelquest.test", "Hero"))
                }
            }
            val viewModel = AuthViewModel(google, repo, FakeUserProfileRepositoryForAuth())

            viewModel.signInWithGoogle(context)
            advanceUntilIdle()

            assertEquals("raw-nonce-1", passedNonce)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun theTokenExchange_sendsTheNonce_toSupabase() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val session = """{"access_token":"a","token_type":"bearer","expires_in":3600,"refresh_token":"r",
            "user":{"id":"3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b","aud":"authenticated","email":"hero@pixelquest.test",
            "app_metadata":{"provider":"google"},"user_metadata":{"full_name":"Hero"}}}"""
        val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
            httpEngine = MockEngine { request ->
                requests += request
                respond(session, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
            install(Auth) {
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
            }
            install(Postgrest)
        }

        val result = AuthRepositoryImpl(supabase.auth, supabase.postgrest).exchangeGoogleIdToken("id-token", "raw-nonce-1")

        assertTrue(result is SupabaseResult.Success)
        val request = requests.single()
        assertEquals("id_token", request.url.parameters["grant_type"])
        val body = Json.parseToJsonElement(
            when (val b = request.body) {
                is TextContent -> b.text
                is OutgoingContent.ByteArrayContent -> String(b.bytes())
                else -> error("unexpected body $b")
            }
        ).jsonObject
        assertEquals("id-token", body["id_token"]!!.jsonPrimitive.content)
        assertEquals("google", body["provider"]!!.jsonPrimitive.content)
        assertEquals("raw-nonce-1", body["nonce"]!!.jsonPrimitive.content)
    }
}
