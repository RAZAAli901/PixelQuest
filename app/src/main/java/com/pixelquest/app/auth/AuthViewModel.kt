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

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val authRepository: AuthRepository,
    private val userProfileRepository: com.pixelquest.app.domain.repository.UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentSession()
        observeAuthChanges()
    }

    private fun checkCurrentSession() {
        viewModelScope.launch {
            val user = authRepository.getInitialUser()
            if (user != null) {
                userProfileRepository.updateSupabaseUserId(user.id)
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
                    userProfileRepository.updateSupabaseUserId(user.id)
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
                    when (val exchangeResult = authRepository.exchangeGoogleIdToken(googleResult.idToken)) {
                        is SupabaseResult.Success -> {
                            val user = exchangeResult.data
                            userProfileRepository.updateSupabaseUserId(user.id)
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
}
