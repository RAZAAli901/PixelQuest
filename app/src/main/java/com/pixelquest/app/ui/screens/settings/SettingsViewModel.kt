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
    val showSimpleModeHighlight: Boolean = false
)

private data class SettingsPrefs(
    val sound: Boolean,
    val crt: Boolean,
    val haptics: Boolean,
    val notifs: Boolean,
    val theme: ThemeMode,
    val simpleMode: Boolean,
    val highlightSeen: Boolean
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val taskRepository: TaskRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler
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
                combine(settingsRepository.simpleModeEnabled, settingsRepository.hasSeenSimpleModeHighlight) { simple, seen -> Pair(simple, seen) }
            ) { notifs, theme, (simple, seen) ->
                Triple(notifs, theme, Pair(simple, seen))
            }
        ) { (sound, crt, haptics), (notifs, theme, pair) ->
            SettingsPrefs(sound, crt, haptics, notifs, theme, pair.first, pair.second)
        }
    ) { profile, difficulty, prefs ->
        val hasUsage = (profile?.totalXp ?: 0) >= 30 || (profile?.level ?: 1) > 1
        val shouldShowHighlight = !prefs.simpleMode && !prefs.highlightSeen && hasUsage
        SettingsUiState(
            profile = profile,
            difficulty = difficulty,
            isSoundEnabled = prefs.sound,
            isCrtEnabled = prefs.crt,
            isHapticsEnabled = prefs.haptics,
            isNotificationsEnabled = prefs.notifs,
            themeMode = prefs.theme,
            isSimpleModeEnabled = prefs.simpleMode,
            showSimpleModeHighlight = shouldShowHighlight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun dismissSimpleModeHighlight() {
        viewModelScope.launch {
            settingsRepository.setSimpleModeHighlightSeen(true)
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

    fun exportBackupToUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val profile = userProfileRepository.getProfile().first()
                val difficulty = difficultySettingsRepository.getCurrentDifficulty().first()
                val tasks = taskRepository.getAllTasks().first()
                val payload = com.pixelquest.app.data.backup.BackupPayload(
                    userProfile = profile,
                    difficultySettings = difficulty,
                    streak = null,
                    tasks = tasks
                )
                val json = com.pixelquest.app.data.backup.DataExportImport.exportToJson(payload)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray())
                }
            } catch (e: Exception) {}
        }
    }

    fun onImportFileSelected(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { isStream ->
                    isStream.bufferedReader().use { it.readText() }
                } ?: return@launch
                val payload = com.pixelquest.app.data.backup.DataExportImport.importFromJson(json)
                // Stores draft payload to trigger confirmation dialog in Step 38
                pendingImportPayload = payload
                _showRestoreConfirmDialog.value = true
            } catch (e: Exception) {}
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val payload = pendingImportPayload ?: return@launch
            payload.userProfile?.let { userProfileRepository.insertProfile(it) }
            payload.difficultySettings?.let { difficultySettingsRepository.insertSettings(it) }
            if (payload.tasks.isNotEmpty()) {
                val currentTasks = taskRepository.getAllTasks().first()
                taskAlarmScheduler.cancelAllAlarms(currentTasks)
                currentTasks.forEach { taskRepository.deleteTask(it) }
                payload.tasks.forEach { taskRepository.insertTask(it) }
                taskAlarmScheduler.rescheduleAllAlarms(payload.tasks)
            }
            pendingImportPayload = null
            _showRestoreConfirmDialog.value = false
        }
    }

    fun dismissImportDialog() {
        pendingImportPayload = null
        _showRestoreConfirmDialog.value = false
    }

    fun updateUsername(newName: String) {
        viewModelScope.launch {
            val current = userProfileRepository.getProfile().first() ?: return@launch
            userProfileRepository.insertProfile(current.copy(username = newName))
        }
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
            val tasks = taskRepository.getAllTasks().first()
            taskAlarmScheduler.cancelAllAlarms(tasks)
            tasks.forEach { taskRepository.deleteTask(it) }
            
            userProfileRepository.insertProfile(
                UserProfileEntity(
                    id = 1,
                    username = "PixelHero",
                    avatarId = "avatar_hero",
                    level = 1,
                    totalXp = 0,
                    perfectDaysTowardNextLevel = 0
                )
            )

            difficultySettingsRepository.insertSettings(
                DifficultySettingsEntity(
                    id = 1,
                    difficultyLevel = com.pixelquest.app.domain.model.DifficultyLevel.MEDIUM,
                    perfectDayThreshold = 0.7f,
                    daysRequiredPerLevel = 7
                )
            )

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
}
