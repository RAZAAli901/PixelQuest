package com.pixelquest.app.auth

import android.app.Application
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.hours

/**
 * The saved session counts as signed in even when it's asked for before it has loaded, as when a
 * worker starts in a fresh process. It used to read as signed out, so the day's AI reminder lines
 * were skipped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AiAccessLoadingTest {

    @Test
    fun aSavedSession_isUsedEvenWhenAskedForWhileItLoads() = runBlocking {
        val saved = UserSession(
            accessToken = "saved-token",
            refreshToken = "r",
            expiresIn = 3600,
            tokenType = "bearer",
            user = null,
            expiresAt = Clock.System.now() + 1.hours
        )
        val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
            httpEngine = MockEngine { respond("{}") }
            install(Auth) {
                sessionManager = MemorySessionManager(saved)
                codeVerifierCache = MemoryCodeVerifierCache()
                autoLoadFromStorage = true // like the app: the session is loaded in the background
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
            }
        }

        // Asked straight away, before the load has finished.
        assertEquals("saved-token", SupabaseAiAccess(supabase.auth).accessToken())
    }
}
