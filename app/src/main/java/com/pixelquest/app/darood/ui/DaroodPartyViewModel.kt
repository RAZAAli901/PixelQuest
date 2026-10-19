package com.pixelquest.app.darood.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Darood Party's home: parties need an account, so it first knows whether one is signed in. */
@HiltViewModel
class DaroodPartyViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    /** Whether an account is signed in; null until the saved session has loaded. */
    val signedIn: StateFlow<Boolean?> = authRepository.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
