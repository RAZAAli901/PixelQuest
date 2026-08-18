package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    fun onNameChanged(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun onDayToggled(day: DayOfWeek) {
        _formState.update { state ->
            val current = state.selectedDays
            val updated = if (current.contains(day)) current - day else current + day
            state.copy(selectedDays = updated, daysError = null)
        }
    }

    fun onTimeSelected(time: LocalTime) {
        _formState.update { it.copy(scheduledTime = time, timeError = null) }
    }

    fun onRecurrenceSelected(recurrence: RecurrenceType) {
        _formState.update { it.copy(recurrenceType = recurrence) }
    }

    fun onCategorySelected(category: TaskCategory) {
        _formState.update { it.copy(category = category) }
    }
}
