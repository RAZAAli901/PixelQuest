package com.pixelquest.app.domain

import com.pixelquest.app.BuildConfig

/**
 * Whether this build has a Supabase project for sign-in, cloud sync and the leaderboard. Builds made
 * without one (the placeholders in app/build.gradle.kts) can't sign in at all: Google's account
 * picker would open and the exchange with the placeholder server would then fail. So they say the
 * features aren't available instead of offering them.
 */
object CloudAvailability {
    private const val PLACEHOLDER_URL = "https://placeholder-project.supabase.co"
    private const val PLACEHOLDER_ANON_KEY = "placeholder-anon-key"

    fun isConfigured(supabaseUrl: String, anonKey: String): Boolean {
        val url = supabaseUrl.trim().trimEnd('/')
        val key = anonKey.trim()
        return url.startsWith("https://") && url != PLACEHOLDER_URL && key.isNotEmpty() && key != PLACEHOLDER_ANON_KEY
    }

    val inThisBuild: Boolean
        get() = isConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)

    const val NOT_IN_THIS_BUILD =
        "Sign-in, cloud sync and the leaderboard aren't set up in this build of PixelQuest. Your progress is saved on this device."
}
