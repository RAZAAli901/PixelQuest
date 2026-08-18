package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.components.TaskItemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class TaskWithStatus(
    val task: TaskEntity,
    val status: TaskItemStatus
)

sealed class TaskUiState {
    object Loading : TaskUiState()
    data class Success(val tasks: List<TaskWithStatus>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository
) : ViewModel() {

    val uiState: StateFlow<TaskUiState> = combine(
        taskRepository.getAllTasks(),
        taskCompletionRepository.getLogsForDate(LocalDate.now())
    ) { tasks, logs ->
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
        TaskUiState.Success(list) as TaskUiState
    }.catch { e ->
        emit(TaskUiState.Error(e.localizedMessage ?: "Failed to load tasks"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState.Loading
    )
}
