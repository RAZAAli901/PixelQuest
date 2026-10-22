package com.pixelquest.app.ui.screens.tasks

import androidx.compose.runtime.Immutable
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.ui.components.TaskItemStatus

@Immutable
data class TaskWithStatus(
    val task: TaskEntity,
    val status: TaskItemStatus,
)

@Immutable
sealed class TaskUiState {
    @Immutable
    object Loading : TaskUiState()

    @Immutable
    data class Success(
        val tasks: List<TaskWithStatus>,
        val completionPercentage: Float = 0f,
        val targetThreshold: Float = 0.7f,
        val isPerfectDay: Boolean = false,
    ) : TaskUiState()

    @Immutable
    data class Error(val message: String) : TaskUiState()
}
