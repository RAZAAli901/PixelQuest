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
        settingsRepository.isSoundEnabled,
        settingsRepository.isCrtEnabled,
        settingsRepository.isNotificationsEnabled
    ) { profile, difficulty, soundEnabled, crtEnabled, notificationsEnabled ->
        SettingsUiState(
            profile = profile,
            difficulty = difficulty,
            isSoundEnabled = soundEnabled,
            isCrtEnabled = crtEnabled,
            isNotificationsEnabled = notificationsEnabled
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

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val current = uiState.value.profile ?: return@launch
            if (newUsername.isNotBlank() && newUsername.length <= 20) {
                userProfileRepository.saveProfile(current.copy(username = newUsername))
            }
        }
    }
}
