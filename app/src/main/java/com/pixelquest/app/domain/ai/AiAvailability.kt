package com.pixelquest.app.domain.ai

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.GeminiProxyClient

/**
 * Whether this build can offer the AI Coach at all. It needs a way to reach Gemini (through the
 * proxy, a real Supabase project; directly, a real key) and, since the AI Coach is for signed-in
 * players, a way to sign in. A release built before the cloud is set up has neither, and the AI
 * Coach could only ever say it isn't set up, so the opt-in isn't offered.
 */
object AiAvailability {

    fun isConfigured(viaProxy: Boolean, supabaseUrl: String, geminiApiKey: String): Boolean =
        if (viaProxy) GeminiProxyClient.isConfigured(supabaseUrl)
        else geminiApiKey.isNotBlank() && geminiApiKey.trim() != "placeholder-gemini-key"

    val inThisBuild: Boolean
        get() = isConfigured(BuildConfig.GEMINI_VIA_PROXY, BuildConfig.SUPABASE_URL, BuildConfig.GEMINI_API_KEY) &&
            com.pixelquest.app.domain.CloudAvailability.inThisBuild
}

/** What the Settings AI Coach card offers. */
enum class AiCoachSetting {
    /** On: offer turning it off (even in a build without AI, so it can always be turned off). */
    ENABLED,
    /** Off, and this build can reach Gemini: offer the opt-in. */
    CAN_OPT_IN,
    /** This build has the AI Coach, but nobody is signed in: offer signing in (it's for signed-in players). */
    NEEDS_SIGN_IN,
    /** Off, and this build has no AI service: say so instead of offering an opt-in that can't work. */
    NOT_IN_THIS_BUILD;

    companion object {
        fun of(isEnabled: Boolean, availableInBuild: Boolean, isSignedIn: Boolean = true): AiCoachSetting = when {
            isEnabled && isSignedIn -> ENABLED
            availableInBuild && !isSignedIn -> NEEDS_SIGN_IN
            isEnabled -> ENABLED
            availableInBuild -> CAN_OPT_IN
            else -> NOT_IN_THIS_BUILD
        }
    }
}
