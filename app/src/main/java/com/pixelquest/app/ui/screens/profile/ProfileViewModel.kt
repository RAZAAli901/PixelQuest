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

import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.flowOf

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val streak: StreakEntity? = null,
    val difficulty: DifficultySettingsEntity? = null,
    val isSoundEnabled: Boolean = true,
    val isCrtEnabled: Boolean = false,
    val isSimpleMode: Boolean = false,
    val totalTasksCompleted: Int = 0,
    val activeTasksCount: Int = 0,
    /** The signed-in account's Darood Party username, without the @; null hides it. */
    val daroodUsername: String? = null,
    /** XP from quests over the last 7 days, today's included. */
    val xpLastWeek: Int = 0,
    /** Signed in: the Leaderboard is open to them. */
    val signedIn: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository? = null,
    private val taskCompletionRepository: TaskCompletionRepository? = null,
    private val myUsername: com.pixelquest.app.darood.data.MyUsername? = null,
    private val authRepository: com.pixelquest.app.auth.AuthRepository? = null,
    private val appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock()
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
        ) { sound, crt, simpleMode -> Triple(sound, crt, simpleMode) },
        combine(
            taskRepository?.getAllTasks() ?: flowOf(emptyList()),
            taskCompletionRepository?.getAllLogs() ?: flowOf(emptyList()),
            appClock.today()
        ) { tasks, logs, today ->
            val week = logs.filter { it.wasCompleted && !it.completedDate.isBefore(today.minusDays(6)) && !it.completedDate.isAfter(today) }
            Triple(tasks.size, logs.count { it.wasCompleted }, week.sumOf { it.pointsAwarded })
        },
        combine(myUsername?.username ?: flowOf(null), authRepository?.currentUser ?: flowOf(null)) { username, user -> username to (user != null) }
    ) { (profile, streak, difficulty), (soundEnabled, crtEnabled, simpleMode), (activeTasksCount, completedLogsCount, xpLastWeek), (username, signedIn) ->
        ProfileUiState(
            profile = profile,
            streak = streak,
            difficulty = difficulty,
            isSoundEnabled = soundEnabled,
            isCrtEnabled = crtEnabled,
            isSimpleMode = simpleMode,
            totalTasksCompleted = completedLogsCount,
            activeTasksCount = activeTasksCount,
            daroodUsername = username?.value,
            xpLastWeek = xpLastWeek,
            signedIn = signedIn
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
