package com.pixelquest.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

enum class HistoryFilter {
    LAST_7_DAYS,
    LAST_30_DAYS,
    ALL_TIME
}

data class TaskHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<TaskHistoryItem> = emptyList(),
    val selectedFilter: HistoryFilter = HistoryFilter.ALL_TIME
)

@HiltViewModel
class TaskHistoryViewModel @Inject constructor(
    private val taskCompletionRepository: TaskCompletionRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL_TIME)

    val uiState: StateFlow<TaskHistoryUiState> = combine(
        taskCompletionRepository.getAllLogs(),
        taskRepository.getAllTasks(),
        _selectedFilter
    ) { logs, tasks, filter ->
        val tasksMap = tasks.associateBy { it.id }

        val today = LocalDate.now()
        val filteredLogs = logs.filter { log ->
            when (filter) {
                HistoryFilter.LAST_7_DAYS -> !log.completedDate.isBefore(today.minusDays(7))
                HistoryFilter.LAST_30_DAYS -> !log.completedDate.isBefore(today.minusDays(30))
                HistoryFilter.ALL_TIME -> true
            }
        }.sortedByDescending { it.completedDate }

        val historyItems = filteredLogs.map { log ->
            val task = tasksMap[log.taskId]
            TaskHistoryItem(
                logId = log.id,
                taskId = log.taskId,
                taskName = task?.name ?: "Unknown Quest #${log.taskId}",
                category = task?.category ?: TaskCategory.OTHER,
                completedDate = log.completedDate,
                wasCompleted = log.wasCompleted,
                pointsAwarded = log.pointsAwarded
            )
        }

        TaskHistoryUiState(
            isLoading = false,
            items = historyItems,
            selectedFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskHistoryUiState(isLoading = true)
    )

    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }
}
