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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?
)

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
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

    override val currentUser: Flow<AuthUser?> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                if (user != null) {
                    AuthUser(
                        id = user.id,
                        email = user.email,
                        displayName = (user.userMetadata?.get("full_name") as? String)
                            ?: (user.userMetadata?.get("name") as? String)
                    )
                } else null
            }
            else -> null
        }
    }

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

    override suspend fun signOut(): SupabaseResult<Unit> = safeSupabaseCall {
        auth.signOut()
    }

    override suspend fun getInitialUser(): AuthUser? {
        val user = auth.currentUserOrNull() ?: return null
        return AuthUser(
            id = user.id,
            email = user.email,
            displayName = (user.userMetadata?.get("full_name") as? String)
                ?: (user.userMetadata?.get("name") as? String)
        )
    }

    override suspend fun deleteAccount(): SupabaseResult<Unit> = safeSupabaseCall {
        postgrest.rpc("delete_user_account")
        auth.signOut()
    }
}
