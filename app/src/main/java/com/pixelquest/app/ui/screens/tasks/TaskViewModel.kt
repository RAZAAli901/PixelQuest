package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.StreakCalculator
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.components.TaskItemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock()
) : ViewModel() {

    /** Today's results; moves on at midnight (see [com.pixelquest.app.util.AppClock.today]). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val todaysLogs = appClock.today().flatMapLatest { taskCompletionRepository.getLogsForDate(it) }

    val uiState: StateFlow<TaskUiState> = combine(
        taskRepository.getAllTasks(),
        todaysLogs,
        difficultySettingsRepository.getCurrentDifficulty()
    ) { tasks, logs, difficulty ->
        val logsMap = logs.associateBy { it.taskId }
        val list = tasks.map { task ->
            val log = logsMap[task.id]
            val status = when {
                log == null -> TaskItemStatus.PENDING
                log.wasCompleted -> TaskItemStatus.COMPLETED
                else -> TaskItemStatus.MISSED
            }
            TaskWithStatus(task, status)
        }
        val targetThreshold = difficulty?.perfectDayThreshold ?: 0.7f
        val pct = StreakCalculator.calculateCompletionPercentage(logs, tasks.size)
        val isPerfect = StreakCalculator.isPerfectDay(pct, targetThreshold)

        TaskUiState.Success(
            tasks = list,
            completionPercentage = pct,
            targetThreshold = targetThreshold,
            isPerfectDay = isPerfect
        ) as TaskUiState
    }.catch { e ->
        emit(TaskUiState.Error(e.localizedMessage ?: "Failed to load tasks"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState.Loading
    )
}
