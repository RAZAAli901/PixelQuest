package com.pixelquest.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.FlavorTextCatalog
import com.pixelquest.app.domain.PointsCalculator
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.components.TaskItemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val streakRepository: StreakRepository,
    private val userProfileRepository: UserProfileRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler
) : ViewModel() {

    private val currentDate: LocalDate = LocalDate.now()

    val uiState: StateFlow<TodayUiState> = combine(
        taskRepository.getTasksForDay(currentDate),
        taskCompletionRepository.getLogsForDate(currentDate),
        streakRepository.getCurrentStreak(),
        userProfileRepository.getProfile(),
        difficultySettingsRepository.getCurrentDifficulty()
    ) { tasks, logs, streak, profile, difficulty ->
        val logMap = logs.associateBy { it.taskId }
        val items = tasks.map { task ->
            val log = logMap[task.id]
            val nowTime = LocalTime.now()
            val status = when {
                log?.wasCompleted == true -> TaskItemStatus.DONE
                log?.wasCompleted == false -> TaskItemStatus.MISSED
                nowTime.isAfter(task.scheduledTime) -> TaskItemStatus.GRACE_PERIOD
                else -> TaskItemStatus.PENDING
            }
            TodayTaskItem(
                task = task,
                status = status,
                scheduledTime = task.scheduledTime
            )
        }
        val sortedItems = items.sortedWith(
            compareBy<TodayTaskItem> { it.status == TaskItemStatus.DONE || it.status == TaskItemStatus.MISSED }
                .thenBy { it.scheduledTime }
        )

        val completedCount = sortedItems.count { it.status == TaskItemStatus.DONE }
        val totalCount = sortedItems.size
        val completionPct = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
        val threshold = difficulty?.perfectDayThreshold ?: 0.7f
        val isPerfectDay = totalCount > 0 && completionPct >= threshold
        val flavorText = FlavorTextCatalog.getFlavorText(totalCount, completedCount, isPerfectDay, currentDate)

        TodayUiState.Success(
            tasks = sortedItems,
            currentStreak = streak?.currentStreak ?: 0,
            totalXp = profile?.totalXp ?: 0,
            level = profile?.level ?: 1,
            perfectDaysTowardNextLevel = profile?.perfectDaysTowardNextLevel ?: 0,
            daysRequiredPerLevel = difficulty?.daysRequiredPerLevel ?: 7,
            completionPercentage = completionPct,
            targetThreshold = threshold,
            isPerfectDay = isPerfectDay,
            flavorText = flavorText
        ) as TodayUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState.Loading
    )

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            taskAlarmScheduler.cancelAlarmForTask(task)
            val currentStreak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0
            val points = PointsCalculator.calculateXpForTask(task, currentStreak)
            val log = TaskCompletionLogEntity(
                taskId = task.id,
                completedDate = currentDate,
                wasCompleted = true,
                pointsAwarded = points
            )
            taskCompletionRepository.insertLog(log)

            val profile = userProfileRepository.getProfile().first()
            if (profile != null) {
                userProfileRepository.updateProfile(profile.copy(totalXp = profile.totalXp + points))
            }
        }
    }

    fun skipTask(task: TaskEntity) {
        viewModelScope.launch {
            taskAlarmScheduler.cancelAlarmForTask(task)
            val log = TaskCompletionLogEntity(
                taskId = task.id,
                completedDate = currentDate,
                wasCompleted = false,
                pointsAwarded = 0
            )
            taskCompletionRepository.insertLog(log)
        }
    }
}
