package com.pixelquest.app.ui.screens.tasks

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.TaskItemStatus

data class TaskWithStatus(
    val task: TaskEntity,
    val status: TaskItemStatus
)

sealed class TaskUiState {
    object Loading : TaskUiState()
    data class Success(
        val tasks: List<TaskWithStatus>,
        val completionPercentage: Float = 0f,
        val targetThreshold: Float = 0.7f,
        val isPerfectDay: Boolean = false
    ) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}
