package com.pixelquest.app.auth

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
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
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth
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
}
