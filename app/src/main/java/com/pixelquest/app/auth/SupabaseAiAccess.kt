package com.pixelquest.app.auth

import com.pixelquest.app.domain.ai.AiAccess
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/** [AiAccess] from the Supabase Auth session (Google or email-code sign-in). */
@Singleton
class SupabaseAiAccess @Inject constructor(private val auth: Auth) : AiAccess {

    override val isSignedIn: Flow<Boolean> = auth.sessionStatus
        // Still loading the saved session: not an answer yet (it would flash "sign in" at launch).
        .filterNot { it is SessionStatus.Initializing }
        // A session whose refresh failed (offline) is still the player's; the proxy decides.
        .map { it is SessionStatus.Authenticated || it is SessionStatus.RefreshFailure }
        .distinctUntilChanged()

    override suspend fun accessToken(): String? {
        // A fresh process (the daily reminder-lines worker, say) can ask before the saved session
        // has loaded; that read as "signed out".
        auth.awaitInitialization()
        val session = auth.currentSessionOrNull() ?: return null
        // The proxy refuses an expired token, so refresh one that is about to expire.
        if (session.expiresAt <= Clock.System.now() + 60.seconds) {
            try {
                auth.refreshCurrentSession()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Offline: the request will fail on its own and say so.
            }
        }
        return auth.currentAccessTokenOrNull()
    }
}
