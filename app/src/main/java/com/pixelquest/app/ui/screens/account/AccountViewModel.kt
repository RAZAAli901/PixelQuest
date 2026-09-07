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
    private val userProfileRepository: UserProfileRepository,
    private val cloudProfileRepository: com.pixelquest.app.data.repository.CloudProfileRepository
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

    fun onDisplayNameChanged(newName: String) {
        val error = validateDisplayName(newName)
        _uiState.value = _uiState.value.copy(
            displayNameInput = newName,
            displayNameError = error
        )
    }

    fun onOptInToggleClicked(targetEnabled: Boolean) {
        if (targetEnabled) {
            val currentName = _uiState.value.displayNameInput
            val error = validateDisplayName(currentName)
            if (error != null) {
                _uiState.value = _uiState.value.copy(displayNameError = error)
                return
            }
            _uiState.value = _uiState.value.copy(showConfirmDialog = true)
        } else {
            optOut()
        }
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun cancelActiveSync() {
        syncJob?.cancel()
        syncJob = null
        _uiState.value = _uiState.value.copy(
            isSyncing = false,
            syncMessage = "Sync cancelled."
        )
    }

    fun confirmOptIn() {
        val name = _uiState.value.displayNameInput.trim()
        val error = validateDisplayName(name)
        if (error != null) {
            _uiState.value = _uiState.value.copy(displayNameError = error, showConfirmDialog = false)
            return
        }

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSyncing = true,
                    showConfirmDialog = false,
                    displayNameError = null
                )
                when (cloudProfileRepository.updateOptInAndSync(optIn = true, displayName = name)) {
                    is com.pixelquest.app.data.remote.SupabaseResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = true,
                            lastSyncTime = System.currentTimeMillis(),
                            syncMessage = "Opt-in complete! Profile synced to cloud."
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = true,
                            syncMessage = "Opt-in saved locally. Cloud sync will retry."
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = "Sync cancelled.")
            }
        }
    }

    fun optOut() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true)
                cloudProfileRepository.updateOptInAndSync(
                    optIn = false,
                    displayName = _uiState.value.displayNameInput.trim()
                )
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    isOptedIn = false,
                    syncMessage = "Opted out from leaderboard."
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = "Sync cancelled.")
            }
        }
    }

    fun syncNow() {
        val state = _uiState.value
        if (!state.isOptedIn) {
            _uiState.value = state.copy(syncMessage = "Opt-in to leaderboard before syncing.")
            return
        }
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
                when (val result = cloudProfileRepository.syncProfileToCloud()) {
                    is com.pixelquest.app.data.remote.SupabaseResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            lastSyncTime = System.currentTimeMillis(),
                            syncMessage = "Cloud sync successful!"
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncMessage = "Network error: check connection."
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.AuthError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncMessage = "Auth error: please sign in again."
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncMessage = "Sync failed: ${result.userMessage}"
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = "Sync cancelled.")
            }
        }
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    companion object {
        fun validateDisplayName(name: String): String? {
            val trimmed = name.trim()
            return when {
                trimmed.isBlank() -> "Display name cannot be blank."
                trimmed.length < 3 -> "Name must be at least 3 characters."
                trimmed.length > 20 -> "Name must not exceed 20 characters."
                !trimmed.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Only alphanumeric characters and underscores allowed."
                else -> null
            }
        }
    }
}
