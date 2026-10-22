package com.pixelquest.app.data.remote

import com.pixelquest.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Singleton providing configured SupabaseClient instances with Postgrest and Auth plugins.
 * Credentials are read securely from BuildConfig (injected from gitignored local.properties).
 */
object SupabaseClientProvider {

    /**
     * Where the sign-in session is kept: set by the app (NetworkModule) before the client is first
     * built, to the Keystore-sealed store (S034). Null keeps supabase-kt's own default.
     */
    @Volatile
    var sessionManager: io.github.jan.supabase.auth.SessionManager? = null

    val client: SupabaseClient by lazy {
        val url = BuildConfig.SUPABASE_URL.ifBlank { "https://placeholder-project.supabase.co" }
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "placeholder-anon-key" }

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            // OkHttp, chosen rather than left to whichever engine is found first: Realtime needs
            // WebSockets, which Ktor's Android engine doesn't have.
            httpEngine = OkHttp.create()
            install(Postgrest)
            install(Auth) {
                SupabaseClientProvider.sessionManager?.let { sessionManager = it }
            }
            // Darood Party's live updates (docs/darood-design.md). Nothing connects until a party is followed.
            install(Realtime)
        }
    }

    val postgrest: Postgrest
        get() = client.postgrest

    val auth: Auth
        get() = client.auth
}
