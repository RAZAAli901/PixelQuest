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

    /**
     * A real Supabase project URL: https, and not the build's placeholder. A debug build may also
     * point at a Supabase stack on the developer's computer (npx supabase start) over plain http;
     * the emulator reaches it as 10.0.2.2. See docs/LOCAL_SUPABASE.md.
     */
    fun isRealProjectUrl(supabaseUrl: String, allowLocalHttp: Boolean = BuildConfig.DEBUG): Boolean {
        val url = supabaseUrl.trim().trimEnd('/')
        if (url.startsWith("https://")) return url != PLACEHOLDER_URL
        return allowLocalHttp && LOCAL_STACK_URL.matches(url)
    }

    private val LOCAL_STACK_URL = Regex("^http://(127\\.0\\.0\\.1|localhost|10\\.0\\.2\\.2)(:\\d{1,5})?$")

    fun isConfigured(supabaseUrl: String, anonKey: String): Boolean {
        val key = anonKey.trim()
        return isRealProjectUrl(supabaseUrl) && key.isNotEmpty() && key != PLACEHOLDER_ANON_KEY
    }

    /**
     * Sign in with Google: a Supabase project and a real Google web client id. Those always end in
     * ".apps.googleusercontent.com"; a placeholder used to show a Google button that could only fail.
     */
    fun isGoogleConfigured(supabaseUrl: String, anonKey: String, googleWebClientId: String): Boolean {
        val clientId = googleWebClientId.trim()
        return isConfigured(supabaseUrl, anonKey) &&
            !clientId.startsWith("placeholder") &&
            clientId.endsWith(".apps.googleusercontent.com")
    }

    val inThisBuild: Boolean
        get() = isConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)

    val googleInThisBuild: Boolean
        get() = isGoogleConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, BuildConfig.GOOGLE_WEB_CLIENT_ID)

    const val NOT_IN_THIS_BUILD =
        "Sign-in, cloud sync and the leaderboard aren't set up in this build of PixelQuest. Your progress is saved on this device."
}
