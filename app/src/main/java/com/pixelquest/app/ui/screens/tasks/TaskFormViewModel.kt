package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.getTaskById(taskId).collect { task ->
                if (task != null) {
                    _formState.update {
                        it.copy(
                            taskId = task.id,
                            name = task.name,
                            description = task.description,
                            scheduledDay = task.scheduledDay,
                            scheduledTime = task.scheduledTime,
                            recurrenceType = task.recurrenceType,
                            category = task.category,
                            isEditMode = true
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _formState.update { state ->
            val error = if (name.isBlank()) "Quest title is required!" else null
            state.copy(name = name, nameError = error)
        }
    }

    fun onDayToggled(day: DayOfWeek) {
        _formState.update { state ->
            val current = state.selectedDays
            val updated = if (current.contains(day)) current - day else current + day
            val error = if (state.recurrenceType == RecurrenceType.WEEKLY && updated.isEmpty()) {
                "Select at least one day!"
            } else null
            state.copy(selectedDays = updated, daysError = error)
        }
    }

    fun onTimeSelected(time: LocalTime?) {
        _formState.update { state ->
            val error = if (time == null) "Time is required!" else null
            state.copy(scheduledTime = time, timeError = error)
        }
    }

    fun onRecurrenceSelected(recurrence: RecurrenceType) {
        _formState.update { state ->
            val daysErr = if (recurrence == RecurrenceType.WEEKLY && state.selectedDays.isEmpty()) {
                "Select at least one day!"
            } else null
            state.copy(recurrenceType = recurrence, daysError = daysErr)
        }
    }

    fun onCategorySelected(category: TaskCategory) {
        _formState.update { it.copy(category = category) }
    }

    fun validateForm(): Boolean {
        val state = _formState.value
        val nameErr = if (state.name.isBlank()) "Quest title is required!" else null
        val timeErr = if (state.scheduledTime == null) "Time is required!" else null
        val daysErr = if (state.recurrenceType == RecurrenceType.WEEKLY && state.selectedDays.isEmpty()) {
            "Select at least one day!"
        } else null

        _formState.update {
            it.copy(
                nameError = nameErr,
                timeError = timeErr,
                daysError = daysErr
            )
        }
        return nameErr == null && timeErr == null && daysErr == null
    }

    fun saveTask(onSuccess: () -> Unit) {
        if (!validateForm()) return
        val state = _formState.value
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            val task = TaskEntity(
                id = state.taskId ?: 0,
                name = state.name.trim(),
                description = state.description,
                scheduledDay = state.scheduledDay,
                scheduledTime = state.scheduledTime ?: LocalTime.of(9, 0),
                recurrenceType = state.recurrenceType,
                category = state.category
            )
            if (state.isEditMode && state.taskId != null) {
                taskRepository.updateTask(task)
            } else {
                taskRepository.insertTask(task)
            }
            _formState.update { it.copy(isSubmitting = false, isSaveSuccess = true) }
            onSuccess()
        }
    }
}
