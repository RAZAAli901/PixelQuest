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
    private val settingsRepository: com.pixelquest.app.domain.repository.SettingsRepository? = null,
    private val syncScheduler: com.pixelquest.app.worker.SyncScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // The account whose name is in the field. Found on the emulator: after another account
            // signed in on the same phone, the field still offered the previous person's public name.
            var nameFieldAccount: String? = null
            userProfileRepository.getProfile().collect { profile ->
                val account = profile?.supabaseUserId
                val otherAccount = account != null && nameFieldAccount != null && account != nameFieldAccount
                if (account != null) nameFieldAccount = account
                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    isOptedIn = profile?.leaderboardOptIn ?: false,
                    displayNameInput = if (otherAccount || _uiState.value.displayNameInput.isBlank()) {
                        profile?.leaderboardDisplayName ?: ""
                    } else _uiState.value.displayNameInput,
                    displayNameError = if (otherAccount) null else _uiState.value.displayNameError
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
                        // The local flag is already off; the sync worker (which opts out when the flag
                        // is off) runs once the device is online, and WorkManager keeps it across restarts.
                        syncScheduler?.scheduleProfileSync(debounceMs = 0L)
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = false,
                            showOptOutSuccessNotice = true,
                            syncMessage = "You've left the leaderboard. Server sync queued when online."
                        )
                    }
                    is com.pixelquest.app.data.remote.SupabaseResult.ServerError,
                    is com.pixelquest.app.data.remote.SupabaseResult.UnknownError -> {
                        // Retried in the background until the server confirms.
                        syncScheduler?.scheduleProfileSync(debounceMs = 0L)
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            isOptedIn = false,
                            showOptOutSuccessNotice = true,
                            syncMessage = "You've left the leaderboard. The server will be updated shortly."
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
                when (val result = cloudProfileRepository.pushProfileToCloud()) {
                    is com.pixelquest.app.data.remote.SupabaseResult.Success -> {
                        // Nothing sent is not a failure, but it isn't a successful sync either: it
                        // used to say "successful" while the leaderboard kept the other numbers.
                        _uiState.value = if (result.data) {
                            _uiState.value.copy(
                                isSyncing = false,
                                isSyncFailed = false,
                                lastSyncTime = System.currentTimeMillis(),
                                syncMessage = SYNC_SUCCESSFUL
                            )
                        } else {
                            _uiState.value.copy(isSyncing = false, isSyncFailed = false, syncMessage = SYNC_SKIPPED_SERVER_AHEAD)
                        }
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

            // 1. Delete profiles row in Supabase. If that fails (e.g. offline), stop: nothing was
            // deleted, so keep the local link and say so instead of claiming success.
            if (cloudProfileRepository.deleteCloudProfile() !is com.pixelquest.app.data.remote.SupabaseResult.Success) {
                _uiState.value = _uiState.value.copy(
                    isDeletingCloudData = false,
                    syncMessage = "Couldn't reach the cloud, so nothing was deleted. Check your connection and try again."
                )
                return@launch
            }

            // 2. Call RPC to delete auth user from Supabase
            val accountResult = authRepository?.deleteAccount()
            if (accountResult != null && accountResult !is com.pixelquest.app.data.remote.SupabaseResult.Success) {
                _uiState.value = _uiState.value.copy(
                    isDeletingCloudData = false,
                    syncMessage = "Your leaderboard entry was deleted, but the account couldn't be. Try again when you're online."
                )
                return@launch
            }

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
        const val SYNC_SUCCESSFUL = "Cloud sync successful!"
        const val SYNC_SKIPPED_SERVER_AHEAD =
            "Nothing sent: the leaderboard already has more progress for this account, from another device."

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
