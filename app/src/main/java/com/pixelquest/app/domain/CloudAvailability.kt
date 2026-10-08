package com.pixelquest.app.domain

import com.pixelquest.app.BuildConfig

/**
 * Whether this build can sign in at all, which cloud sync, the leaderboard and the AI Coach need: a
 * Supabase project (then email-code sign-in works). Sign in with Google also needs the Google web
 * client id, so its button only shows when the build has one. Builds made without a project (the
 * placeholders in app/build.gradle.kts) say the features aren't available instead of offering them.
 */
object CloudAvailability {
    private const val PLACEHOLDER_URL = "https://placeholder-project.supabase.co"
    private const val PLACEHOLDER_ANON_KEY = "placeholder-anon-key"

    /** A real Supabase project URL: https, and not the build's placeholder. */
    fun isRealProjectUrl(supabaseUrl: String): Boolean {
        val url = supabaseUrl.trim().trimEnd('/')
        return url.startsWith("https://") && url != PLACEHOLDER_URL
    }

    fun isConfigured(supabaseUrl: String, anonKey: String): Boolean {
        val key = anonKey.trim()
        return isRealProjectUrl(supabaseUrl) && key.isNotEmpty() && key != PLACEHOLDER_ANON_KEY
    }

    /** Sign in with Google: a Supabase project and the Google web client id. */
    fun isGoogleConfigured(supabaseUrl: String, anonKey: String, googleWebClientId: String): Boolean =
        isConfigured(supabaseUrl, anonKey) && googleWebClientId.isNotBlank()

    val inThisBuild: Boolean
        get() = isConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)

    val googleInThisBuild: Boolean
        get() = isGoogleConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, BuildConfig.GOOGLE_WEB_CLIENT_ID)

    const val NOT_IN_THIS_BUILD =
        "Sign-in, cloud sync and the leaderboard aren't set up in this build of PixelQuest. Your progress is saved on this device."
}
