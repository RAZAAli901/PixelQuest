package com.pixelquest.app.domain.ai

/**
 * What the AI Coach tells the user when a Gemini request fails. Raw HTTP bodies, exception text and
 * parser output never reach the screen; each failure maps to one plain sentence that says what
 * happened and what to do.
 */
object AiErrorCopy {
    const val UNAVAILABLE = "The AI coach isn't available right now. Your quests and progress are safe. Try again later."
    const val NOT_CONFIGURED = "The AI coach isn't set up in this build of PixelQuest. Your quests and progress are safe."
    const val BUSY = "Gemini is busy right now. Try again in a few minutes."
    const val DAILY_LIMIT = "The AI coach has used up today's requests. Try again tomorrow."
    const val SERVER_TROUBLE = "Gemini is having trouble right now. Try again in a few minutes."
    const val TIMEOUT = "Gemini took too long to answer. Check your internet connection and try again."
    const val OFFLINE = "Can't reach Gemini. Check your internet connection and try again."
    const val UNREADABLE = "The AI coach's answer came back garbled. Try again in a few minutes."
    const val UNKNOWN = "Something went wrong while getting your insight. Try again later."
    const val SIGN_IN_REQUIRED = "The AI coach is for signed-in players. Sign in with Google or an emailed code in Account to use it."

    /**
     * Message for a Gemini HTTP status. 401 is what PixelQuest's own clients report before sending
     * anything when the build has no Gemini key or no Supabase project for the proxy: [NOT_CONFIGURED].
     * 400 and 403 mean the key was rejected and 404 means the model id is wrong, which the user can't
     * fix, so they share [UNAVAILABLE]. A 200 that still failed means the response could not be read.
     */
    fun forStatus(statusCode: Int?): String = when (statusCode) {
        401 -> NOT_CONFIGURED
        400, 403, 404 -> UNAVAILABLE
        429 -> BUSY
        in 500..599 -> SERVER_TROUBLE
        200 -> UNREADABLE
        else -> UNKNOWN
    }
}
