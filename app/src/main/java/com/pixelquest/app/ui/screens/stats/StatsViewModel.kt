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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

            // Today counts once something is done (or the day ends); until then it isn't a day missed.
            val totalScheduledDays = weekDays.values.count { it != DailyStatus.NO_TASKS_SCHEDULED && it != DailyStatus.IN_PROGRESS }
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
    private val settingsRepository: SettingsRepository? = null,
    private val appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock()
) : ViewModel() {

    /** The last 3 months up to today; moves on at midnight (see [com.pixelquest.app.util.AppClock.today]). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val rangeData = appClock.today().flatMapLatest { today ->
        val startDate = today.minusMonths(3)
        combine(
            statsRepository.getCompletionRateOverRange(startDate, today),
            statsRepository.getDailyStatusForRange(startDate, today),
            settingsRepository?.simpleModeEnabled ?: flowOf(false)
        ) { rate, dailyStatusMap, isSimpleMode -> RangeData(today, rate, dailyStatusMap, isSimpleMode) }
    }

    val uiState: StateFlow<StatsUiState> = combine(
        combine(
            streakRepository.getCurrentStreak(),
            userProfileRepository.getProfile(),
            difficultySettingsRepository.getCurrentDifficulty()
        ) { streak, profile, difficulty -> Triple(streak, profile, difficulty) },
        rangeData
    ) { (streak, profile, difficulty), (today, rate, dailyStatusMap, isSimpleMode) ->
        val weeklyTrend = StatsDataBucketer.calculateWeeklyBuckets(dailyStatusMap, today)
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

private data class RangeData(
    val today: LocalDate,
    val completionRate: Float,
    val dailyStatus: Map<LocalDate, DailyStatus>,
    val isSimpleMode: Boolean
)
