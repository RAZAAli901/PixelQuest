package com.pixelquest.app.auth

import android.app.Application
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Signing out with no connection signs you out on this device. supabase-kt threw before forgetting
 * the session, so the app said "signed out" while the AI Coach kept working, and the next launch
 * signed the account back in (on a shared phone, for the next person).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SignOutOfflineTest {

    private var online = true
    private val session = """{"access_token":"a","token_type":"bearer","expires_in":3600,"refresh_token":"r",
        "user":{"id":"3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b","aud":"authenticated","email":"hero@pixelquest.test","app_metadata":{},"user_metadata":{}}}"""

    private val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
        httpEngine = MockEngine {
            if (!online) throw IOException("no network")
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
    private val repository = AuthRepositoryImpl(supabase.auth, supabase.postgrest)

    @Test
    fun signingOutOffline_stillForgetsTheSessionOnThisDevice() = runBlocking {
        repository.verifyEmailCode("hero@pixelquest.test", "123456")
        val access = SupabaseAiAccess(supabase.auth)
        assertEquals("a", access.accessToken())

        online = false
        repository.signOut()

        assertNull(supabase.auth.currentSessionOrNull())
        assertNull("The AI Coach stops", access.accessToken())
        assertEquals(false, withTimeout(5_000) { access.isSignedIn.first() })
        assertNull(repository.getInitialUser())
    }
}
