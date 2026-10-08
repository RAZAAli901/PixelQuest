package com.pixelquest.app.domain.ai

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.data.remote.GeminiProxyClient

/**
 * Whether this build can reach Gemini at all: through the proxy it needs a real Supabase project,
 * directly it needs a real key. A release built before the proxy is set up has neither, and the AI
 * Coach could only ever say it isn't set up, so the opt-in isn't offered.
 */
object AiAvailability {

    fun isConfigured(viaProxy: Boolean, supabaseUrl: String, geminiApiKey: String): Boolean =
        if (viaProxy) GeminiProxyClient.isConfigured(supabaseUrl)
        else geminiApiKey.isNotBlank() && geminiApiKey.trim() != "placeholder-gemini-key"

    val inThisBuild: Boolean
        get() = isConfigured(BuildConfig.GEMINI_VIA_PROXY, BuildConfig.SUPABASE_URL, BuildConfig.GEMINI_API_KEY)
}

/** What the Settings AI Coach card offers. */
enum class AiCoachSetting {
    /** On: offer turning it off (even in a build without AI, so it can always be turned off). */
    ENABLED,
    /** Off, and this build can reach Gemini: offer the opt-in. */
    CAN_OPT_IN,
    /** Off, and this build has no AI service: say so instead of offering an opt-in that can't work. */
    NOT_IN_THIS_BUILD;

    companion object {
        fun of(isEnabled: Boolean, availableInBuild: Boolean): AiCoachSetting = when {
            isEnabled -> ENABLED
            availableInBuild -> CAN_OPT_IN
            else -> NOT_IN_THIS_BUILD
        }
    }
}
