package com.pixelquest.app.auth

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?
)

interface AuthRepository {
    /** The signed-in account, or null when signed out. Nothing is sent while the saved session loads. */
    val currentUser: Flow<AuthUser?>

    /**
     * True while the account is signed in but its session couldn't be refreshed (offline, or the
     * server failing). It is still the player's account: sync, the leaderboard and the AI Coach
     * come back with the connection.
     */
    val serverUnreachable: Flow<Boolean> get() = flowOf(false)
    suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String? = null): SupabaseResult<AuthUser>
    suspend fun signOut(): SupabaseResult<Unit>
    suspend fun getInitialUser(): AuthUser?
    suspend fun deleteAccount(): SupabaseResult<Unit> = SupabaseResult.Success(Unit)

    /** Emails a one-time sign-in code to [email], making the account the first time (see EmailSignIn). */
    suspend fun sendEmailCode(email: String): SupabaseResult<Unit> =
        SupabaseResult.AuthError(UnsupportedOperationException("Email sign-in"), "Email sign-in isn't available.")

    /** Signs in with the code emailed to [email]. */
    suspend fun verifyEmailCode(email: String, code: String): SupabaseResult<AuthUser> =
        SupabaseResult.AuthError(UnsupportedOperationException("Email sign-in"), "Email sign-in isn't available.")
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = auth.sessionStatus
        // Still loading the saved session: not an answer yet (it read as signed out).
        .filterNot { it is SessionStatus.Initializing }
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.toAuthUser()
                // The refresh failed (offline, say): supabase-kt keeps the session saved but no
                // longer current, and Account used to say "signed out" until the connection was back.
                is SessionStatus.RefreshFailure -> savedUser()
                else -> null
            }
        }

    override val serverUnreachable: Flow<Boolean> = auth.sessionStatus
        .filterNot { it is SessionStatus.Initializing }
        .map { it is SessionStatus.RefreshFailure }
        .distinctUntilChanged()

    private suspend fun savedUser(): AuthUser? = try {
        auth.sessionManager.loadSession()?.user?.toAuthUser()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun UserInfo.toAuthUser() = AuthUser(
        id = id,
        email = email,
        displayName = (userMetadata?.get("full_name") as? String) ?: (userMetadata?.get("name") as? String)
    )

    override suspend fun sendEmailCode(email: String): SupabaseResult<Unit> = safeSupabaseCall {
        auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    override suspend fun verifyEmailCode(email: String, code: String): SupabaseResult<AuthUser> = safeSupabaseCall {
        auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
        val user = auth.currentUserOrNull() ?: throw IllegalStateException("No session after the code was accepted")
        AuthUser(
            id = user.id,
            email = user.email,
            displayName = (user.userMetadata?.get("full_name") as? String) ?: (user.userMetadata?.get("name") as? String)
        )
    }

    override suspend fun exchangeGoogleIdToken(
        idToken: String,
        rawNonce: String?
    ): SupabaseResult<AuthUser> = safeSupabaseCall {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            if (rawNonce != null) {
                this.nonce = rawNonce
            }
        }
        val user = auth.currentUserOrNull()
            ?: throw IllegalStateException("Supabase user session missing after token exchange")
        AuthUser(
            id = user.id,
            email = user.email,
            displayName = (user.userMetadata?.get("full_name") as? String)
                ?: (user.userMetadata?.get("name") as? String)
        )
    }

    override suspend fun signOut(): SupabaseResult<Unit> {
        val result = safeSupabaseCall { auth.signOut() }
        if (result !is SupabaseResult.Success) {
            // Offline (or a server error), supabase-kt throws before it forgets the session, which
            // then came back on the next launch and kept the AI Coach working. Forget it on this
            // device anyway; the session on the server just expires.
            try {
                auth.clearSession()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        return result
    }

    override suspend fun getInitialUser(): AuthUser? {
        // Asked as the screen opens, before the saved session has loaded, this said signed out.
        auth.awaitInitialization()
        return auth.currentUserOrNull()?.toAuthUser()
            ?: if (auth.sessionStatus.value is SessionStatus.RefreshFailure) savedUser() else null
    }

    override suspend fun deleteAccount(): SupabaseResult<Unit> = safeSupabaseCall {
        postgrest.rpc("delete_user_account")
        auth.signOut()
    }
}
