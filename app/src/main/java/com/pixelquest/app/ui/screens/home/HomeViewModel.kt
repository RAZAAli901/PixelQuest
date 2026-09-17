package com.pixelquest.app.ui.screens.home

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

import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf

data class HomeUiState(
    val profile: UserProfileEntity? = null,
    val streak: StreakEntity? = null,
    val difficulty: DifficultySettingsEntity? = null,
    val pendingLevelUp: Int? = null,
    val isSimpleMode: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val levelUpSignalManager: LevelUpSignalManager,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        userProfileRepository.getProfile(),
        streakRepository.getCurrentStreak(),
        difficultySettingsRepository.getCurrentDifficulty(),
        levelUpSignalManager.pendingLevelUp,
        settingsRepository?.simpleModeEnabled ?: flowOf(false)
    ) { profile, streak, difficulty, pendingLevelUp, isSimpleMode ->
        HomeUiState(
            profile = profile,
            streak = streak,
            difficulty = difficulty,
            pendingLevelUp = if (isSimpleMode) null else pendingLevelUp,
            isSimpleMode = isSimpleMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun dismissLevelUpCelebration() {
        levelUpSignalManager.clearPendingLevelUp()
    }
}
