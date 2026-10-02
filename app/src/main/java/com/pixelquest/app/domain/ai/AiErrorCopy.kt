package com.pixelquest.app.domain.ai

/**
 * What the AI Coach tells the user when a Gemini request fails. Raw HTTP bodies, exception text and
 * parser output never reach the screen; each failure maps to one plain sentence that says what
 * happened and what to do.
 */
object AiErrorCopy {
    const val UNAVAILABLE = "The AI coach isn't available right now. Your quests and progress are safe. Try again later."
    const val BUSY = "Gemini is busy right now. Try again in a few minutes."
    const val SERVER_TROUBLE = "Gemini is having trouble right now. Try again in a few minutes."
    const val TIMEOUT = "Gemini took too long to answer. Check your internet connection and try again."
    const val OFFLINE = "Can't reach Gemini. Check your internet connection and try again."
    const val UNREADABLE = "The AI coach's answer came back garbled. Try again in a few minutes."
    const val UNKNOWN = "Something went wrong while getting your insight. Try again later."

    /**
     * Message for a Gemini HTTP status. 400, 401 and 403 mean the key was rejected and 404 means the
     * model id is wrong, which the user can't fix, so they share [UNAVAILABLE]. A 200 that still
     * failed means the response could not be read.
     */
    fun forStatus(statusCode: Int?): String = when (statusCode) {
        400, 401, 403, 404 -> UNAVAILABLE
        429 -> BUSY
        in 500..599 -> SERVER_TROUBLE
        200 -> UNREADABLE
        else -> UNKNOWN
    }
}
