package com.pixelquest.app.data.remote

import com.pixelquest.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth

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
            install(Postgrest)
            install(Auth)
        }
    }

    val postgrest: Postgrest
        get() = client.postgrest

    val auth: Auth
        get() = client.auth
}
