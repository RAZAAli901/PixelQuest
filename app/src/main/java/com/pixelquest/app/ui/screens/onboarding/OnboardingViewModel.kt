package com.pixelquest.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.pixelquest.app.domain.model.DifficultyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingStep {
    object Welcome : OnboardingStep
    object NameEntry : OnboardingStep
    object AvatarPick : OnboardingStep
    object DifficultyPick : OnboardingStep
    object Summary : OnboardingStep
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val username: String = "",
    val avatarId: String = "avatar_hero",
    val difficultyLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val isNameValid: Boolean = false,
    val nameError: String? = null,
    /** What the backup picked on the welcome screen holds, while its restore waits to be confirmed. */
    val restoreSummary: String? = null,
    /** Why a picked file couldn't be restored. */
    val restoreMessage: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val backupRestorer: com.pixelquest.app.data.backup.BackupRestorer? = null,
    private val autoBackups: com.pixelquest.app.data.backup.AutoBackups? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Whether the welcome screen offers to restore a backup instead (a reinstall, or a new phone). */
    val canRestoreBackup: Boolean get() = backupRestorer != null

    private var pendingBackup: com.pixelquest.app.data.backup.BackupPayload? = null

    /**
     * The newest automatic backup kept on the phone, if any. Onboarding with one means the database
     * was lost (Android deletes a corrupt one) while the backups survived, so the welcome screen offers it.
     */
    val latestAutoBackup: java.time.LocalDate? = if (backupRestorer != null) autoBackups?.days()?.firstOrNull() else null

    fun onAutoBackupSelected(day: java.time.LocalDate) {
        val payload = autoBackups?.read(day)?.let { com.pixelquest.app.data.backup.DataExportImport.importFromJson(it) }
        if (payload == null || !payload.isUsable) {
            _uiState.value = _uiState.value.copy(restoreMessage = RESTORE_FAILED, restoreSummary = null)
            return
        }
        pendingBackup = payload
        _uiState.value = _uiState.value.copy(
            restoreSummary = com.pixelquest.app.data.backup.BackupSummary.describe(payload),
            restoreMessage = null
        )
    }

    /**
     * A backup picked on the welcome screen. The upgrade path from 1.1.0 is export, reinstall,
     * import, and the import used to wait until onboarding had made a new hero to replace.
     */
    fun onBackupFileSelected(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val payload = try {
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?.let { com.pixelquest.app.data.backup.DataExportImport.importFromJson(it) }
            } catch (e: Exception) {
                null
            }
            if (payload == null || !payload.isUsable) {
                _uiState.value = _uiState.value.copy(restoreMessage = NOT_A_BACKUP, restoreSummary = null)
                return@launch
            }
            pendingBackup = payload
            _uiState.value = _uiState.value.copy(
                restoreSummary = com.pixelquest.app.data.backup.BackupSummary.describe(payload),
                restoreMessage = null
            )
        }
    }

    fun dismissRestore() {
        pendingBackup = null
        _uiState.value = _uiState.value.copy(restoreSummary = null)
    }

    /** Restores the picked backup; its hero replaces onboarding, which is then done. */
    fun confirmRestore(onRestored: () -> Unit = {}) {
        val payload = pendingBackup ?: return
        viewModelScope.launch {
            try {
                backupRestorer?.restore(payload)
                settingsRepository.setOnboardingComplete(true)
                pendingBackup = null
                _uiState.value = _uiState.value.copy(restoreSummary = null)
                onRestored()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(restoreSummary = null, restoreMessage = RESTORE_FAILED)
            }
        }
    }

    companion object {
        const val NOT_A_BACKUP = "That file isn't a PixelQuest backup. Pick another, or start a new hero."
        const val RESTORE_FAILED = "The restore didn't finish. Try again, or start a new hero."
    }

    fun updateUsername(name: String) {
        val trimmed = com.pixelquest.app.domain.HeroName.clamp(name)
        val rule = com.pixelquest.app.domain.HeroName.error(trimmed)
        val isValid = rule == null
        // No error before the player has typed anything.
        val error = if (trimmed.isEmpty()) null else rule
        _uiState.update {
            it.copy(username = trimmed, isNameValid = isValid, nameError = error)
        }
    }

    fun updateAvatar(avatarId: String) {
        _uiState.update { it.copy(avatarId = avatarId) }
    }

    fun updateDifficulty(difficultyLevel: DifficultyLevel) {
        _uiState.update { it.copy(difficultyLevel = difficultyLevel) }
    }

    fun goToStep(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun nextStep() {
        val next = when (_uiState.value.currentStep) {
            OnboardingStep.Welcome -> OnboardingStep.NameEntry
            OnboardingStep.NameEntry -> if (_uiState.value.isNameValid) OnboardingStep.AvatarPick else _uiState.value.currentStep
            OnboardingStep.AvatarPick -> OnboardingStep.DifficultyPick
            OnboardingStep.DifficultyPick -> OnboardingStep.Summary
            OnboardingStep.Summary -> OnboardingStep.Summary
        }
        _uiState.update { it.copy(currentStep = next) }
    }

    fun previousStep() {
        val prev = when (_uiState.value.currentStep) {
            OnboardingStep.Welcome -> OnboardingStep.Welcome
            OnboardingStep.NameEntry -> OnboardingStep.Welcome
            OnboardingStep.AvatarPick -> OnboardingStep.NameEntry
            OnboardingStep.DifficultyPick -> OnboardingStep.AvatarPick
            OnboardingStep.Summary -> OnboardingStep.DifficultyPick
        }
        _uiState.update { it.copy(currentStep = prev) }
    }

    fun completeOnboarding(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            // After RESET ALL PROGRESS the existing profile keeps this install's cloud link and
            // leaderboard choice; carry them over. A fresh profile here used to drop them, so the old
            // public row stayed on the leaderboard while Account said the player wasn't opted in.
            val existing = userProfileRepository.getProfile().first()
            // Onboarding names the hero; it never takes progress away. An existing player's level and
            // XP stay (after RESET ALL PROGRESS they're already a new player's). It used to write
            // level 1 and 0 XP over a player sent here by an update (see OnboardingGate).
            val userProfile = com.pixelquest.app.data.local.entity.UserProfileEntity(
                id = 1,
                username = state.username.ifBlank { "PixelHero" },
                avatarId = state.avatarId,
                level = existing?.level ?: 1,
                totalXp = existing?.totalXp ?: 0,
                perfectDaysTowardNextLevel = existing?.perfectDaysTowardNextLevel ?: 0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                supabaseUserId = existing?.supabaseUserId,
                leaderboardOptIn = existing?.leaderboardOptIn ?: false,
                leaderboardDisplayName = existing?.leaderboardDisplayName
            )
            userProfileRepository.insertProfile(userProfile)

            val diffThreshold = com.pixelquest.app.domain.DifficultyMode.getPerfectDayThreshold(state.difficultyLevel)
            val daysReq = com.pixelquest.app.domain.DifficultyMode.getDaysRequiredPerLevel(state.difficultyLevel)
            val diffEntity = com.pixelquest.app.data.local.entity.DifficultySettingsEntity(
                id = 1,
                difficultyLevel = state.difficultyLevel,
                perfectDayThreshold = diffThreshold,
                daysRequiredPerLevel = daysReq
            )
            difficultySettingsRepository.insertSettings(diffEntity)

            settingsRepository.setOnboardingComplete(true)
            onSuccess()
        }
    }
}
