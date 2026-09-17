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
    val lastSyncTime: Long? = null,
    val isSyncFailed: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showDeleteDoubleConfirmDialog: Boolean = false,
    val showOptOutConfirmDialog: Boolean = false,
    val showOptOutSuccessNotice: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val isDeletingCloudData: Boolean = false,
    val isSimpleModeEnabled: Boolean = false,
    val isLeaderboardAllowedUnderSimpleMode: Boolean = true
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val cloudProfileRepository: com.pixelquest.app.data.repository.CloudProfileRepository,
    private val authRepository: com.pixelquest.app.auth.AuthRepository? = null,
    private val settingsRepository: com.pixelquest.app.domain.repository.SettingsRepository? = null
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
        viewModelScope.launch {
            settingsRepository?.simpleModeEnabled?.collect { simpleMode ->
                val allowed = com.pixelquest.app.domain.policy.LeaderboardSimpleModePolicy.isLeaderboardAllowed(simpleMode)
                _uiState.value = _uiState.value.copy(
                    isSimpleModeEnabled = simpleMode,
                    isLeaderboardAllowedUnderSimpleMode = allowed
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
            requestOptOut()
        }
    }

    fun requestOptOut() {
        _uiState.value = _uiState.value.copy(showOptOutConfirmDialog = true)
    }

    fun dismissOptOutDialog() {
        _uiState.value = _uiState.value.copy(showOptOutConfirmDialog = false)
    }

    fun confirmOptOut() {
        _uiState.value = _uiState.value.copy(showOptOutConfirmDialog = false)
        optOut()
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

    fun dismissOptOutSuccessNotice() {
        _uiState.value = _uiState.value.copy(showOptOutSuccessNotice = false)
    }

    fun showPrivacyPolicy() {
        _uiState.value = _uiState.value.copy(showPrivacyDialog = true)
    }

    fun dismissPrivacyPolicy() {
        _uiState.value = _uiState.value.copy(showPrivacyDialog = false)
    }

    fun optOut() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
                when (val result = cloudProfileRepository.optOutFromLeaderboard()) {
                    is com.pixelquest.app.data.remote.SupabaseResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = false,
                            showOptOutSuccessNotice = true,
                            lastSyncTime = System.currentTimeMillis(),
                            syncMessage = "You've left the leaderboard. Your rank and display name are now private."
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = false,
                            showOptOutSuccessNotice = true,
                            syncMessage = "You've left the leaderboard. Server sync queued when online."
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = false,
                            showOptOutSuccessNotice = true,
                            syncMessage = "You've left the leaderboard."
                        )
                    }
                }
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
                            isSyncFailed = false,
                            lastSyncTime = System.currentTimeMillis(),
                            syncMessage = "Cloud sync successful!"
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.NetworkError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isSyncFailed = true,
                            syncMessage = "Network error: check connection."
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.AuthError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isSyncFailed = true,
                            syncMessage = "Auth error: please sign in again."
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.ServerError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isSyncFailed = true,
                            syncMessage = "Server error: ${result.message}"
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.UnknownError -> {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isSyncFailed = true,
                            syncMessage = "Sync failed: ${result.message}"
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

    fun requestDeleteCloudAccount() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }

    fun proceedToDeleteDoubleConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false,
            showDeleteDoubleConfirmDialog = true
        )
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false,
            showDeleteDoubleConfirmDialog = false
        )
    }

    fun confirmDeleteCloudAccount(authViewModel: com.pixelquest.app.auth.AuthViewModel? = null) {
        dismissDeleteDialog()
        syncJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeletingCloudData = true,
                syncMessage = null
            )

            // 1. Delete profiles row in Supabase
            cloudProfileRepository.deleteCloudProfile()

            // 2. Call RPC to delete auth user from Supabase
            authRepository?.deleteAccount()

            // 3. Clear local Room database cloud fields
            userProfileRepository.clearCloudData()

            // 4. Sign out locally via AuthViewModel
            authViewModel?.signOut()

            _uiState.value = _uiState.value.copy(
                isDeletingCloudData = false,
                isOptedIn = false,
                displayNameInput = "",
                displayNameError = null,
                syncMessage = "Cloud data and account deleted successfully."
            )
        }
    }

    companion object {
        fun validateDisplayName(name: String): String? {
            val trimmed = name.trim()
            return when {
                trimmed.isBlank() -> "Display name cannot be blank."
                trimmed.length < 3 -> "Name must be at least 3 characters."
                trimmed.length > 20 -> "Name must not exceed 20 characters."
                !trimmed.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Only alphanumeric characters and underscores allowed."
                else -> com.pixelquest.app.domain.DisplayNameModerator.validate(trimmed)
            }
        }
    }
}
