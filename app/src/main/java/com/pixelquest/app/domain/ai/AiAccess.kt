package com.pixelquest.app.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * The AI Coach is for signed-in players (Google, or a code sent by email). This is how the app asks
 * whether someone is signed in, and gets the token the gemini-proxy checks.
 */
interface AiAccess {
    /** Whether an account is signed in. Doesn't emit until the saved session has been loaded. */
    val isSignedIn: Flow<Boolean>

    /** The signed-in account's access token, refreshed when it's about to expire; null when signed out. */
    suspend fun accessToken(): String?
}
