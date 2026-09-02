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
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfileEntity? = null,
    val difficulty: DifficultySettingsEntity? = null,
    val isSoundEnabled: Boolean = true,
    val isCrtEnabled: Boolean = false,
    val isHapticsEnabled: Boolean = true,
    val isNotificationsEnabled: Boolean = true
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
            settingsRepository.isSoundEnabled,
            settingsRepository.isCrtEnabled,
            settingsRepository.isHapticsEnabled,
            settingsRepository.isNotificationsEnabled
        ) { sound, crt, haptics, notifs ->
            arrayOf(sound, crt, haptics, notifs)
        }
    ) { profile, difficulty, prefs ->
        SettingsUiState(
            profile = profile,
            difficulty = difficulty,
            isSoundEnabled = prefs[0],
            isCrtEnabled = prefs[1],
            isHapticsEnabled = prefs[2],
            isNotificationsEnabled = prefs[3]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

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
}
