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
import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
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
    val heatmapStatusMap: Map<LocalDate, DailyStatus> = emptyMap(),
    val weeklyTrend: List<Pair<String, Float>> = emptyList(),
    val isSimpleMode: Boolean = false
)

object StatsDataBucketer {
    fun calculateWeeklyBuckets(
        statusMap: Map<LocalDate, DailyStatus>,
        today: LocalDate = LocalDate.now(),
        weeksCount: Int = 4
    ): List<Pair<String, Float>> {
        return (weeksCount - 1 downTo 0).map { weeksAgo ->
            val weekEnd = today.minusWeeks(weeksAgo.toLong())
            val weekStart = weekEnd.minusDays(6)
            val weekDays = statusMap.filterKeys { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }

            val totalScheduledDays = weekDays.values.count { it != DailyStatus.NO_TASKS_SCHEDULED }
            val completedDays = weekDays.values.count { it == DailyStatus.PERFECT || it == DailyStatus.PARTIAL }

            val rate = if (totalScheduledDays == 0) 0f else (completedDays.toFloat() / totalScheduledDays.toFloat()).coerceIn(0f, 1f)
            val label = if (weeksAgo == 0) "NOW" else "W-$weeksAgo"
            Pair(label, rate)
        }
    }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val startDate = LocalDate.now().minusMonths(3)
    private val endDate = LocalDate.now()

    val uiState: StateFlow<StatsUiState> = combine(
        combine(
            streakRepository.getCurrentStreak(),
            userProfileRepository.getProfile(),
            difficultySettingsRepository.getCurrentDifficulty()
        ) { streak, profile, difficulty -> Triple(streak, profile, difficulty) },
        combine(
            statsRepository.getCompletionRateOverRange(startDate, endDate),
            statsRepository.getDailyStatusForRange(startDate, endDate),
            settingsRepository?.simpleModeEnabled ?: flowOf(false)
        ) { rate, dailyStatusMap, isSimpleMode -> Triple(rate, dailyStatusMap, isSimpleMode) }
    ) { (streak, profile, difficulty), (rate, dailyStatusMap, isSimpleMode) ->
        val weeklyTrend = StatsDataBucketer.calculateWeeklyBuckets(dailyStatusMap)
        StatsUiState(
            currentStreak = streak?.currentStreak ?: 0,
            longestStreak = streak?.longestStreak ?: 0,
            totalPoints = profile?.totalXp ?: 0,
            overallCompletionRate = rate,
            difficultyLevel = difficulty?.difficultyLevel ?: DifficultyLevel.MEDIUM,
            heatmapStatusMap = dailyStatusMap,
            weeklyTrend = weeklyTrend,
            isSimpleMode = isSimpleMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
