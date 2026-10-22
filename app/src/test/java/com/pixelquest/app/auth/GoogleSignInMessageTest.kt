package com.pixelquest.app.auth

import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertEquals
import org.junit.Test

/** What a player sees when Google sign-in fails before it reaches the server. */
class GoogleSignInMessageTest {

    @Test
    fun noGoogleAccountOnThePhone_saysToAddOne() {
        // Found on the emulator: the sign-in said only "No credentials available".
        assertEquals(
            GoogleAuthManager.NO_GOOGLE_ACCOUNT,
            GoogleAuthManager.failureMessage(NoCredentialException("No credentials available"))
        )
    }

    @Test
    fun aNetworkFailure_saysSo() {
        assertEquals(
            "No internet connection detected. Please connect to the internet to sign in.",
            GoogleAuthManager.failureMessage(GetCredentialUnknownException("network error"))
        )
    }

    @Test
    fun anythingElse_getsAPlainSentence_notAndroidsText() {
        // S056: Android's own text isn't shown.
        assertEquals(com.pixelquest.app.domain.SafeErrorCopy.GOOGLE_FAILED, GoogleAuthManager.failureMessage(GetCredentialUnknownException("Something odd")))
    }
}
