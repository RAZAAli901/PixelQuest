package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
}
