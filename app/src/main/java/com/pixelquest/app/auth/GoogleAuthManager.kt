package com.pixelquest.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pixelquest.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleAuthResult {
    data class Success(
        val idToken: String,
        val email: String,
        val displayName: String?
    ) : GoogleAuthResult()

    data class Cancelled(val message: String = "Sign-in was cancelled.") : GoogleAuthResult()
    data class Failure(val exception: Throwable, val message: String) : GoogleAuthResult()
}

/**
 * Manages Google Sign-In using modern Android Credential Manager and Google Identity Services.
 */
@Singleton
open class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    open suspend fun signInWithGoogle(activityContext: Context): GoogleAuthResult {
        if (!com.pixelquest.app.util.NetworkUtils.isOnline(activityContext)) {
            return GoogleAuthResult.Failure(
                java.io.IOException("No internet connection"),
                "No internet connection detected. Please connect to the internet to sign in."
            )
        }

        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (serverClientId.isBlank() || serverClientId.startsWith("placeholder")) {
            return GoogleAuthResult.Failure(
                IllegalStateException("GOOGLE_WEB_CLIENT_ID is not configured"),
                "Google Client ID is not configured in local.properties."
            )
        }

        val rawNonce = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            val credential = result.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleAuthResult.Success(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName
                )
            } else {
                GoogleAuthResult.Failure(
                    IllegalStateException("Unexpected credential type: ${credential.type}"),
                    "Received unrecognized credential type."
                )
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled("Sign-in cancelled by user.")
        } catch (e: GetCredentialException) {
            val isNetworkErr = e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("connection", ignoreCase = true) == true ||
                    e.cause is java.io.IOException
            if (isNetworkErr) {
                GoogleAuthResult.Failure(e, "No internet connection detected. Please connect to the internet to sign in.")
            } else {
                GoogleAuthResult.Failure(e, e.message ?: "Google Sign-In failed.")
            }
        } catch (e: java.io.IOException) {
            GoogleAuthResult.Failure(e, "No internet connection detected. Please connect to the internet to sign in.")
        } catch (e: Exception) {
            GoogleAuthResult.Failure(e, e.message ?: "An unexpected error occurred during Google Sign-In.")
        }
    }

    open suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Ignored on clear failure
        }
    }
}
