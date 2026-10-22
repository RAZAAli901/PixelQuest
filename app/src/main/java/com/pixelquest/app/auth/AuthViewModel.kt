package com.pixelquest.app.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object SignedOut : AuthUiState()
    object SigningIn : AuthUiState()
    data class SignedIn(
        val user: AuthUser,
        /** Signed in, but the session can't be refreshed right now (see AuthRepository.serverUnreachable). */
        val serverUnreachable: Boolean = false
    ) : AuthUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AuthUiState()
}

/** The email-code sign-in form on Account (see [EmailSignIn]). */
data class EmailSignInState(
    val isOpen: Boolean = false,
    val email: String = "",
    val code: String = "",
    /** The address the last code went to; null until one was sent. */
    val codeSentTo: String? = null,
    val isWorking: Boolean = false,
    val error: String? = null,
    /** Seconds until another code can be asked for for [countdownFor]. */
    val resendInSeconds: Long = 0,
    /** The address the countdown is for: Supabase allows one code a minute per address, not per phone. */
    val countdownFor: String? = null
) {
    /** Whether a code can be asked for [email] now. */
    fun canSendTo(email: String): Boolean =
        !isWorking && !(resendInSeconds > 0 && countdownFor == EmailSignIn.normalizeEmail(email))
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val authRepository: AuthRepository,
    private val userProfileRepository: com.pixelquest.app.domain.repository.UserProfileRepository,
    private val accountLink: CloudAccountLink? = null,
    private val cloudProfiles: com.pixelquest.app.data.repository.CloudProfileRepository? = null,
    private val pendingChoice: com.pixelquest.app.data.repository.PendingLeaderboardChoice? = null,
    private val daroodParties: com.pixelquest.app.darood.data.DaroodPartyRepository? = null,
    private val study: com.pixelquest.app.study.data.StudyRepository? = null
) : ViewModel() {

    /** Links [accountId] to this device's profile; a different account than last time starts fresh (see CloudAccountLink). */
    private suspend fun linkAccount(accountId: String) {
        if (accountLink != null) accountLink.onSignedIn(accountId) else userProfileRepository.updateSupabaseUserId(accountId)
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _emailState = MutableStateFlow(EmailSignInState())
    val emailState: StateFlow<EmailSignInState> = _emailState.asStateFlow()
    private var resendTicker: kotlinx.coroutines.Job? = null
    /** The send or code check in progress, so CANCEL can stop it. */
    private var emailRequest: kotlinx.coroutines.Job? = null

    init {
        checkCurrentSession()
        observeAuthChanges()
    }

    private fun checkCurrentSession() {
        viewModelScope.launch {
            val user = authRepository.getInitialUser()
            if (user != null) {
                linkAccount(user.id)
                _uiState.value = AuthUiState.SignedIn(user)
            } else {
                _uiState.value = AuthUiState.SignedOut
            }
        }
    }

    private fun observeAuthChanges() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(authRepository.currentUser, authRepository.serverUnreachable) { user, unreachable ->
                user to unreachable
            }.collect { (user, unreachable) ->
                if (user != null) {
                    linkAccount(user.id)
                    _uiState.value = AuthUiState.SignedIn(user, serverUnreachable = unreachable)
                } else if (_uiState.value !is AuthUiState.SigningIn && _uiState.value !is AuthUiState.Error) {
                    _uiState.value = AuthUiState.SignedOut
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.SigningIn
            when (val googleResult = googleAuthManager.signInWithGoogle(activityContext)) {
                is GoogleAuthResult.Success -> {
                    when (val exchangeResult = authRepository.exchangeGoogleIdToken(googleResult.idToken, googleResult.rawNonce)) {
                        is SupabaseResult.Success -> {
                            val user = exchangeResult.data
                            linkAccount(user.id)
                            _uiState.value = AuthUiState.SignedIn(user)
                        }
                        is SupabaseResult.NetworkError -> {
                            googleAuthManager.signOut()
                            authRepository.signOut()
                            userProfileRepository.updateSupabaseUserId(null)
                            _uiState.value = AuthUiState.Error(
                                "Google authentication succeeded, but cloud connection failed. Please check your internet connection."
                            )
                        }
                        is SupabaseResult.AuthError -> {
                            googleAuthManager.signOut()
                            authRepository.signOut()
                            userProfileRepository.updateSupabaseUserId(null)
                            _uiState.value = AuthUiState.Error(
                                "Google authentication succeeded, but cloud token exchange failed. Please verify Supabase OAuth provider settings."
                            )
                        }
                        is SupabaseResult.ServerError -> {
                            googleAuthManager.signOut()
                            authRepository.signOut()
                            userProfileRepository.updateSupabaseUserId(null)
                            _uiState.value = AuthUiState.Error(
                                "Google authentication succeeded, but cloud server error occurred: ${exchangeResult.message}"
                            )
                        }
                        is SupabaseResult.UnknownError -> {
                            googleAuthManager.signOut()
                            authRepository.signOut()
                            userProfileRepository.updateSupabaseUserId(null)
                            _uiState.value = AuthUiState.Error(
                                "Google authentication succeeded, but cloud token exchange failed: ${exchangeResult.message}"
                            )
                        }
                    }
                }
                is GoogleAuthResult.Cancelled -> {
                    _uiState.value = AuthUiState.SignedOut
                }
                is GoogleAuthResult.Failure -> {
                    _uiState.value = AuthUiState.Error(googleResult.message)
                }
            }
        }
    }

    private var authJob: kotlinx.coroutines.Job? = null

    fun cancelActiveAuth() {
        authJob?.cancel()
        authJob = null
        _uiState.value = AuthUiState.SignedOut
    }

    /**
     * A leave from the leaderboard made on this phone and not yet sent can only be sent while signed
     * in. Signing out without it left the player public on the board until they signed in again.
     */
    private suspend fun sendPendingLeave() {
        val accountId = (_uiState.value as? AuthUiState.SignedIn)?.user?.id
            ?: userProfileRepository.getProfile().first()?.supabaseUserId
            ?: return
        if (pendingChoice?.pendingFor(accountId)?.optIn != false) return
        try {
            cloudProfiles?.optOutFromLeaderboard()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            // Still pending: sent after the next sign-in to this account.
        }
    }

    fun signOut(onSignedOut: (() -> Unit)? = null) {
        authJob?.cancel()
        authJob = viewModelScope.launch {
            sendPendingLeave()
            // Darood entries still waiting go now, while there's a sign-in to send them with: signing
            // out clears them from the phone (DaroodAccountCleanup).
            try {
                daroodParties?.sendPending()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {}
            // So do study ticks and study time.
            try {
                study?.sendPending()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {}
            try {
                authRepository.signOut()
            } catch (_: Exception) {}
            try {
                googleAuthManager.signOut()
            } catch (_: Exception) {}
            try {
                userProfileRepository.updateSupabaseUserId(null)
            } catch (_: Exception) {}
            _uiState.value = AuthUiState.SignedOut
            onSignedOut?.invoke()
        }
    }

    fun dismissError() {
        _uiState.value = AuthUiState.SignedOut
    }

    // Sign in with a code sent by email.

    fun openEmailSignIn() {
        // Trying email after a failed Google sign-in: the Google error no longer applies.
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.SignedOut
        _emailState.value = _emailState.value.copy(isOpen = true, error = null)
    }

    fun closeEmailSignIn() {
        // A send or a code check still running stops here: CANCEL used to let a pending check
        // sign you in anyway. The countdown carries on (Supabase still counts that minute).
        emailRequest?.cancel()
        val state = _emailState.value
        _emailState.value = EmailSignInState(resendInSeconds = state.resendInSeconds, countdownFor = state.countdownFor)
    }

    fun onEmailChanged(raw: String) {
        val email = com.pixelquest.app.domain.validation.InputRules.typed(raw, com.pixelquest.app.domain.validation.InputRules.Field.EMAIL)
        _emailState.value = _emailState.value.copy(email = email, error = null)
    }

    fun onCodeChanged(code: String) {
        // Digits only, and no longer than the longest code Supabase sends.
        _emailState.value = _emailState.value.copy(code = EmailSignIn.normalizeCode(code).take(10), error = null)
    }

    /** Back to the address field, to fix a typo or use another address (which can get a code at once). */
    fun useDifferentEmail() {
        emailRequest?.cancel()
        _emailState.value = _emailState.value.copy(codeSentTo = null, code = "", error = null, isWorking = false)
    }

    fun sendEmailCode() {
        val state = _emailState.value
        val email = EmailSignIn.normalizeEmail(state.email)
        if (!state.canSendTo(email)) return
        if (!EmailSignIn.isValidEmail(email)) {
            _emailState.value = state.copy(error = EmailSignIn.INVALID_EMAIL)
            return
        }
        _emailState.value = state.copy(isWorking = true, error = null)
        emailRequest = viewModelScope.launch {
            val result = authRepository.sendEmailCode(email)
            if (result is SupabaseResult.Success) {
                _emailState.value = _emailState.value.copy(
                    isWorking = false, email = email, codeSentTo = email, code = "",
                    resendInSeconds = EmailSignIn.RESEND_AFTER_SECONDS, countdownFor = email
                )
                startResendCountdown()
            } else {
                _emailState.value = _emailState.value.copy(isWorking = false, error = EmailSignIn.sendFailure(result))
            }
        }
    }

    fun verifyEmailCode() {
        val state = _emailState.value
        val email = state.codeSentTo ?: return
        if (state.isWorking) return
        if (!EmailSignIn.isValidCode(state.code)) {
            _emailState.value = state.copy(error = EmailSignIn.INVALID_CODE)
            return
        }
        _emailState.value = state.copy(isWorking = true, error = null)
        emailRequest = viewModelScope.launch {
            when (val result = authRepository.verifyEmailCode(email, EmailSignIn.normalizeCode(state.code))) {
                is SupabaseResult.Success -> {
                    linkAccount(result.data.id)
                    _uiState.value = AuthUiState.SignedIn(result.data)
                    closeEmailSignIn()
                }
                else -> _emailState.value = _emailState.value.copy(isWorking = false, error = EmailSignIn.verifyFailure(result))
            }
        }
    }

    /** Counts [EmailSignInState.resendInSeconds] down to 0, one second at a time. */
    private fun startResendCountdown() {
        resendTicker?.cancel()
        resendTicker = viewModelScope.launch {
            while (_emailState.value.resendInSeconds > 0) {
                kotlinx.coroutines.delay(1_000)
                _emailState.value = _emailState.value.copy(resendInSeconds = _emailState.value.resendInSeconds - 1)
            }
        }
    }
}
