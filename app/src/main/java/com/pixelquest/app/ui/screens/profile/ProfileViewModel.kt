package com.pixelquest.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val streak: StreakEntity? = null,
    val difficulty: DifficultySettingsEntity? = null,
    val isSoundEnabled: Boolean = true,
    val isCrtEnabled: Boolean = false,
    val isSimpleMode: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(
            userProfileRepository.getProfile(),
            streakRepository.getCurrentStreak(),
            difficultySettingsRepository.getCurrentDifficulty()
        ) { profile, streak, difficulty -> Triple(profile, streak, difficulty) },
        combine(
            settingsRepository.isSoundEnabled,
            settingsRepository.isCrtEnabled,
            settingsRepository.simpleModeEnabled
        ) { sound, crt, simpleMode -> Triple(sound, crt, simpleMode) }
    ) { (profile, streak, difficulty), (soundEnabled, crtEnabled, simpleMode) ->
        ProfileUiState(
            profile = profile,
            streak = streak,
            difficulty = difficulty,
            isSoundEnabled = soundEnabled,
            isCrtEnabled = crtEnabled,
            isSimpleMode = simpleMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
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
}
