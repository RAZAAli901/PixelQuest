package com.pixelquest.app.ui.screens.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.domain.repository.StatsRepository
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TaskAnalyticsUiState(
    val task: TaskEntity? = null,
    val stats: PerTaskStats = PerTaskStats(0L, 0, 0, 0f, 0, 0)
)

@HiltViewModel
class TaskAnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val statsRepository: StatsRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    val uiState: StateFlow<TaskAnalyticsUiState> = if (taskId == 0L) {
        flowOf(TaskAnalyticsUiState())
    } else {
        combine(
            taskRepository.getTaskById(taskId),
            statsRepository.getPerTaskStats(taskId)
        ) { task, stats ->
            TaskAnalyticsUiState(
                task = task,
                stats = stats
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskAnalyticsUiState()
    )
}
