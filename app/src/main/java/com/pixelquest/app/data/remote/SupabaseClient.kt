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
            install(Auth)
            // Darood Party's live updates (docs/darood-design.md). Nothing connects until a party is followed.
            install(Realtime)
        }
    }

    val postgrest: Postgrest
        get() = client.postgrest

    val auth: Auth
        get() = client.auth
}
