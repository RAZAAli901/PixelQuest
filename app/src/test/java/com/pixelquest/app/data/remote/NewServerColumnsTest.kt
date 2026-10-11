package com.pixelquest.app.data.remote

import android.app.Application
import com.pixelquest.app.data.repository.LeaderboardRepositoryImpl
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
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A column the server adds later doesn't break the leaderboard in apps already installed: the app
 * reads whole profile rows, and its Supabase client (set up like SupabaseClientProvider's) ignores
 * columns it doesn't know. Kept as a guard for supabase-kt upgrades, which could change that.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NewServerColumnsTest {

    @Test
    fun aRowWithAColumnTheAppDoesntKnow_stillReads() = runBlocking {
        val row = """[{"id":"p1","display_name":"Titan_Slayer","current_streak":12,"longest_streak":20,
            "level":4,"total_xp":1500,"leaderboard_opt_in":true,"updated_at":"2026-10-11T09:00:00Z",
            "a_column_from_a_newer_server":"anything"}]"""
        val supabase = createSupabaseClient("https://abcd1234.supabase.co", "sb_publishable_test") {
            httpEngine = MockEngine { respond(row, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
            install(Postgrest)
            install(Auth) {
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
            }
        }

        val top = LeaderboardRepositoryImpl(supabase.postgrest, supabase.auth).getTopByStreak(limit = 20, offset = 0)

        assertTrue("$top", top is SupabaseResult.Success)
        assertEquals("Titan_Slayer", (top as SupabaseResult.Success).data.single().displayName)
    }
}
