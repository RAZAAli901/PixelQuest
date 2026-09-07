package com.pixelquest.app.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val profile: UserProfileEntity? = null,
    val isOptedIn: Boolean = false,
    val displayNameInput: String = "",
    val displayNameError: String? = null,
    val showConfirmDialog: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastSyncTime: Long? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    isOptedIn = profile?.leaderboardOptIn ?: false,
                    displayNameInput = if (_uiState.value.displayNameInput.isBlank()) {
                        profile?.leaderboardDisplayName ?: ""
                    } else _uiState.value.displayNameInput
                )
            }
        }
    }

    fun onOptInToggleClicked(targetEnabled: Boolean) {
        if (targetEnabled) {
            _uiState.value = _uiState.value.copy(showConfirmDialog = true)
        } else {
            viewModelScope.launch {
                userProfileRepository.updateLeaderboardOptIn(false)
                _uiState.value = _uiState.value.copy(isOptedIn = false)
            }
        }
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }
}
