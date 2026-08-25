package com.pixelquest.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StatsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPoints: Int = 0,
    val overallCompletionRate: Float = 0f,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val heatmapStatusMap: Map<LocalDate, DailyStatus> = emptyMap()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository
) : ViewModel() {

    private val startDate = LocalDate.now().minusMonths(3)
    private val endDate = LocalDate.now()

    val uiState: StateFlow<StatsUiState> = combine(
        streakRepository.getCurrentStreak(),
        userProfileRepository.getProfile(),
        difficultySettingsRepository.getCurrentDifficulty(),
        statsRepository.getCompletionRateOverRange(startDate, endDate),
        statsRepository.getDailyStatusForRange(startDate, endDate)
    ) { streak, profile, difficulty, rate, dailyStatusMap ->
        StatsUiState(
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0,
            totalPoints = profile?.totalXp ?: 0,
            overallCompletionRate = rate,
            difficultyLevel = difficulty?.difficultyLevel ?: DifficultyLevel.MEDIUM,
            heatmapStatusMap = dailyStatusMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
