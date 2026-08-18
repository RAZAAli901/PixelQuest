package com.pixelquest.app.ui.screens.tasks

import com.pixelquest.app.data.local.entity.TaskEntity

sealed class TaskUiState {
    object Loading : TaskUiState()
    data class Success(val tasks: List<TaskEntity>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}
