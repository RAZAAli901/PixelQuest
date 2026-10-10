package com.pixelquest.app.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object SignedOut : AuthUiState()
    object SigningIn : AuthUiState()
    data class SignedIn(
        val user: AuthUser
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
    /** Seconds until another code can be asked for. */
    val resendInSeconds: Long = 0
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val authRepository: AuthRepository,
    private val userProfileRepository: com.pixelquest.app.domain.repository.UserProfileRepository,
    private val accountLink: CloudAccountLink? = null
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
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    linkAccount(user.id)
                    _uiState.value = AuthUiState.SignedIn(user)
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

    fun signOut(onSignedOut: (() -> Unit)? = null) {
        authJob?.cancel()
        authJob = viewModelScope.launch {
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
        _emailState.value = _emailState.value.copy(isOpen = true, error = null)
    }

    fun closeEmailSignIn() {
        resendTicker?.cancel()
        _emailState.value = EmailSignInState()
    }

    fun onEmailChanged(email: String) {
        _emailState.value = _emailState.value.copy(email = email, error = null)
    }

    fun onCodeChanged(code: String) {
        // Digits only, and no longer than the longest code Supabase sends.
        _emailState.value = _emailState.value.copy(code = EmailSignIn.normalizeCode(code).take(10), error = null)
    }

    /** Back to the address field, to fix a typo or use another address. */
    fun useDifferentEmail() {
        _emailState.value = _emailState.value.copy(codeSentTo = null, code = "", error = null)
    }

    fun sendEmailCode() {
        val state = _emailState.value
        if (state.isWorking || state.resendInSeconds > 0) return
        val email = EmailSignIn.normalizeEmail(state.email)
        if (!EmailSignIn.isValidEmail(email)) {
            _emailState.value = state.copy(error = EmailSignIn.INVALID_EMAIL)
            return
        }
        _emailState.value = state.copy(isWorking = true, error = null)
        viewModelScope.launch {
            val result = authRepository.sendEmailCode(email)
            if (result is SupabaseResult.Success) {
                _emailState.value = _emailState.value.copy(
                    isWorking = false, email = email, codeSentTo = email, code = "",
                    resendInSeconds = EmailSignIn.RESEND_AFTER_SECONDS
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
        viewModelScope.launch {
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
