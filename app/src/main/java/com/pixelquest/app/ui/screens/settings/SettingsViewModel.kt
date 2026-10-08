package com.pixelquest.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.pixelquest.app.ui.theme.ThemeMode
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfileEntity? = null,
    val difficulty: DifficultySettingsEntity? = null,
    val isSoundEnabled: Boolean = true,
    val isCrtEnabled: Boolean = false,
    val isHapticsEnabled: Boolean = true,
    val isNotificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.Pixel,
    val isSimpleModeEnabled: Boolean = false,
    val showSimpleModeHighlight: Boolean = false,
    val showComicModeHighlight: Boolean = false,
    val isAiInsightsEnabled: Boolean = false
)

private data class SettingsPrefs(
    val sound: Boolean,
    val crt: Boolean,
    val haptics: Boolean,
    val notifs: Boolean,
    val theme: ThemeMode,
    val simpleMode: Boolean,
    val highlightSeen: Boolean,
    val comicHighlightSeen: Boolean,
    val aiInsights: Boolean
)

private data class PrefsSubGroup(
    val notifs: Boolean,
    val theme: ThemeMode,
    val simple: Boolean,
    val highlights: Pair<Boolean, Boolean>,
    val aiInsights: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val taskRepository: TaskRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val progressReset: com.pixelquest.app.data.local.ProgressReset,
    private val backupRestorer: com.pixelquest.app.data.backup.BackupRestorer
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userProfileRepository.getProfile(),
        difficultySettingsRepository.getCurrentDifficulty(),
        combine(
            combine(
                settingsRepository.isSoundEnabled,
                settingsRepository.isCrtEnabled,
                settingsRepository.isHapticsEnabled
            ) { sound, crt, haptics -> Triple(sound, crt, haptics) },
            combine(
                settingsRepository.isNotificationsEnabled,
                settingsRepository.themeMode,
                settingsRepository.aiInsightsEnabled,
                combine(
                    settingsRepository.simpleModeEnabled,
                    settingsRepository.hasSeenSimpleModeHighlight,
                    settingsRepository.hasSeenComicModeHighlight
                ) { simple, seen, comicSeen -> Triple(simple, seen, comicSeen) }
            ) { notifs, theme, aiEnabled, (simple, seen, comicSeen) ->
                PrefsSubGroup(notifs, theme, simple, Pair(seen, comicSeen), aiEnabled)
            }
        ) { (sound, crt, haptics), subGroup ->
            SettingsPrefs(
                sound = sound,
                crt = crt,
                haptics = haptics,
                notifs = subGroup.notifs,
                theme = subGroup.theme,
                simpleMode = subGroup.simple,
                highlightSeen = subGroup.highlights.first,
                comicHighlightSeen = subGroup.highlights.second,
                aiInsights = subGroup.aiInsights
            )
        }
    ) { profile, difficulty, prefs ->
        val hasUsage = (profile?.totalXp ?: 0) >= 30 || (profile?.level ?: 1) > 1
        val shouldShowHighlight = !prefs.simpleMode && !prefs.highlightSeen && hasUsage
        val shouldShowComicHighlight = !prefs.comicHighlightSeen && prefs.theme != ThemeMode.Comic
        SettingsUiState(
            profile = profile,
            difficulty = difficulty,
            isSoundEnabled = prefs.sound,
            isCrtEnabled = prefs.crt,
            isHapticsEnabled = prefs.haptics,
            isNotificationsEnabled = prefs.notifs,
            themeMode = prefs.theme,
            isSimpleModeEnabled = prefs.simpleMode,
            showSimpleModeHighlight = shouldShowHighlight,
            showComicModeHighlight = shouldShowComicHighlight,
            isAiInsightsEnabled = prefs.aiInsights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    /** False on Android 12+ until the user allows "Alarms & reminders"; reminders may then run late. */
    fun canScheduleExactAlarms(): Boolean = taskAlarmScheduler.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): android.content.Intent = taskAlarmScheduler.openExactAlarmSettingsIntent()

    /** Off sends reminders to the silent channel (see NotificationChannels.reminderChannel). */
    val isReminderSoundEnabled: StateFlow<Boolean> = settingsRepository.isNotificationSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** The setting existed (and the theme read it) but nothing could switch it on. */
    val isReduceMotionEnabled: StateFlow<Boolean> = settingsRepository.isReduceMotionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setReduceMotionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceMotionEnabled(enabled)
        }
    }

    fun setReminderSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationSoundEnabled(enabled)
        }
    }

    val isAiReminderMessagesEnabled: StateFlow<Boolean> = settingsRepository.aiReminderMessagesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAiReminderMessagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiReminderMessagesEnabled(enabled)
        }
    }

    fun setAiInsightsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiInsightsEnabled(enabled)
        }
    }

    fun dismissSimpleModeHighlight() {
        viewModelScope.launch {
            settingsRepository.setSimpleModeHighlightSeen(true)
        }
    }

    fun dismissComicModeHighlight() {
        viewModelScope.launch {
            settingsRepository.setComicModeHighlightSeen(true)
        }
    }

    fun toggleSimpleMode(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                settingsRepository.setSimpleModeHighlightSeen(true)
            }
            settingsRepository.setSimpleModeEnabled(enabled)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            if (mode == ThemeMode.Comic) {
                settingsRepository.setComicModeHighlightSeen(true)
            }
            settingsRepository.setThemeMode(mode)
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }

    fun toggleCrt(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrtEnabled(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            val tasks = taskRepository.getAllTasks().first()
            if (!enabled) {
                taskAlarmScheduler.cancelAllAlarms(tasks)
            } else {
                taskAlarmScheduler.rescheduleAllAlarms(tasks)
            }
        }
    }

    // One line under the backup buttons saying what happened; saving, reading and restoring used to
    // fail (or succeed) silently, and an unreadable file still opened the restore dialog.
    private val _backupMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun exportBackupToUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _backupMessage.value = try {
                // Includes the streak and completion history (the streak used to be left out).
                val payload = backupRestorer.snapshot()
                val json = com.pixelquest.app.data.backup.DataExportImport.exportToJson(payload)
                val written = context.contentResolver.openOutputStream(uri)?.use { os -> os.write(json.toByteArray()) } != null
                if (written) BackupMessages.SAVED else BackupMessages.SAVE_FAILED
            } catch (e: Exception) {
                BackupMessages.SAVE_FAILED
            }
        }
    }

    fun onImportFileSelected(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { isStream ->
                    isStream.bufferedReader().use { it.readText() }
                } ?: return@launch
                val payload = com.pixelquest.app.data.backup.DataExportImport.importFromJson(json)
                if (!payload.isUsable) {
                    _backupMessage.value = BackupMessages.NOT_A_BACKUP
                    return@launch
                }
                _backupMessage.value = null
                // Stores draft payload to trigger confirmation dialog in Step 38
                pendingImportPayload = payload
                _showRestoreConfirmDialog.value = true
            } catch (e: Exception) {
                _backupMessage.value = BackupMessages.READ_FAILED
            }
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val payload = pendingImportPayload ?: return@launch
            _backupMessage.value = try {
                backupRestorer.restore(payload)
                BackupMessages.RESTORED
            } catch (e: Exception) {
                BackupMessages.RESTORE_FAILED
            }
            pendingImportPayload = null
            _showRestoreConfirmDialog.value = false
        }
    }

    fun dismissImportDialog() {
        pendingImportPayload = null
        _showRestoreConfirmDialog.value = false
    }

    // The hero name being edited, or null when the field shows the saved name. Typing only changes
    // the draft; SAVE NAME stores it. (Every keystroke used to be written to the database, without
    // validation, with the field's text coming back from that write.)
    private val _nameDraft = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val nameDraft: StateFlow<String?> = _nameDraft.asStateFlow()

    fun onNameDraftChanged(text: String) {
        _nameDraft.value = com.pixelquest.app.domain.HeroName.clamp(text)
    }

    /** Saves the draft if it's a valid name. Returns false (and keeps the draft) if it isn't. */
    fun saveName(): Boolean {
        val draft = _nameDraft.value ?: return true
        if (com.pixelquest.app.domain.HeroName.error(draft) != null) return false
        viewModelScope.launch {
            val current = userProfileRepository.getProfile().first() ?: return@launch
            userProfileRepository.insertProfile(current.copy(username = draft.trim()))
            _nameDraft.value = null
        }
        return true
    }

    fun onResetProgressClicked() {
        _resetStep.value = 1
    }

    fun advanceResetStep() {
        _resetStep.value = 2
    }

    fun cancelReset() {
        _resetStep.value = 0
    }

    fun performFullReset(onResetComplete: () -> Unit) {
        viewModelScope.launch {
            progressReset.resetAll()
            settingsRepository.setOnboardingComplete(false)
            _resetStep.value = 0
            onResetComplete()
        }
    }

    private val _resetStep = kotlinx.coroutines.flow.MutableStateFlow(0)
    val resetStep: StateFlow<Int> = _resetStep.asStateFlow()

    private var pendingImportPayload: com.pixelquest.app.data.backup.BackupPayload? = null
    private val _showRestoreConfirmDialog = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showRestoreConfirmDialog: StateFlow<Boolean> = _showRestoreConfirmDialog.asStateFlow()

    private val _showSimpleModeDialog = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showSimpleModeDialog: StateFlow<Boolean> = _showSimpleModeDialog.asStateFlow()

    fun requestEnableSimpleMode() {
        _showSimpleModeDialog.value = true
    }

    fun dismissSimpleModeDialog() {
        _showSimpleModeDialog.value = false
    }

    fun confirmEnableSimpleMode() {
        _showSimpleModeDialog.value = false
        toggleSimpleMode(true)
    }

    private val _showAiInsightsPrivacyDialog = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showAiInsightsPrivacyDialog: StateFlow<Boolean> = _showAiInsightsPrivacyDialog.asStateFlow()

    fun requestEnableAiInsights() {
        _showAiInsightsPrivacyDialog.value = true
    }

    fun dismissAiInsightsPrivacyDialog() {
        _showAiInsightsPrivacyDialog.value = false
    }

    fun confirmEnableAiInsights() {
        _showAiInsightsPrivacyDialog.value = false
        setAiInsightsEnabled(true)
    }

    fun disableAiInsights() {
        setAiInsightsEnabled(false)
    }
}

/** What the backup buttons report. */
object BackupMessages {
    const val SAVED = "Backup saved."
    const val SAVE_FAILED = "Couldn't save the backup. Try another location."
    const val NOT_A_BACKUP = "That file isn't a PixelQuest backup. Nothing was changed."
    const val READ_FAILED = "Couldn't read that file. Nothing was changed."
    const val RESTORED = "Backup restored."
    const val RESTORE_FAILED = "The restore didn't finish. Try again."
}
