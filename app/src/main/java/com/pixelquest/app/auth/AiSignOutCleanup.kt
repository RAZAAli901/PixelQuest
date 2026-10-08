package com.pixelquest.app.auth

import com.pixelquest.app.data.local.prefs.EncouragementPackStore
import com.pixelquest.app.domain.ai.AiAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * The AI Coach is for signed-in players, so once nobody is signed in, AI-written reminder lines saved
 * earlier aren't used any more (they'd otherwise last until tomorrow); reminders use the built-in lines.
 */
object AiSignOutCleanup {
    /** [access] is resolved on [scope], so the Supabase client isn't built on the main thread at launch. */
    fun start(scope: CoroutineScope, access: () -> AiAccess, packs: EncouragementPackStore): Job = scope.launch {
        try {
            access().isSignedIn.filter { signedIn -> !signedIn }.collect { packs.clear() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("AiSignOutCleanup", "Couldn't watch the sign-in state", e)
        }
    }
}
