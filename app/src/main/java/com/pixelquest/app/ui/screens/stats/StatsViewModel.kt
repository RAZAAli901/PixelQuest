package com.pixelquest.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val streak: StreakEntity? = null,
    val difficulty: DifficultySettingsEntity? = null,
    val last7DaysLogs: List<Pair<LocalDate, Boolean>> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val taskCompletionRepository: TaskCompletionRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        streakRepository.getCurrentStreak(),
        difficultySettingsRepository.getCurrentDifficulty(),
        taskCompletionRepository.getAllLogs()
    ) { streak, difficulty, logs ->
        val today = LocalDate.now()
        val last7Days = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateLogs = logs.filter { it.completedAt.toLocalDate() == date }
            val completedCount = dateLogs.count { it.wasCompleted }
            val isPerfect = completedCount > 0 // Simple 7-day strip status indicator
            Pair(date, isPerfect)
        }

        StatsUiState(
            streak = streak,
            difficulty = difficulty,
            last7DaysLogs = last7Days
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
