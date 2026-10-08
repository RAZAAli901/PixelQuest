package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.QuestStart
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock()
) : ViewModel() {

    private val _formState = MutableStateFlow(withStart(TaskFormState()))

    /** Refreshes [TaskFormState.startsTomorrow] for a new quest; edited quests keep their start day. */
    private fun withStart(state: TaskFormState): TaskFormState {
        val time = state.scheduledTime
        val startsTomorrow = !state.isEditMode && time != null && QuestStart.startsTomorrow(appClock.now(), time)
        return state.copy(startsTomorrow = startsTomorrow)
    }
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    private var loadedTaskId: Long? = null

    /**
     * Fills the form from the stored task once. Calling it again for the same task (the screen does
     * after a rotation) keeps the player's unsaved edits, and later database changes don't overwrite them.
     */
    fun loadTask(taskId: Long) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId
        viewModelScope.launch {
            taskRepository.getTaskById(taskId).first()?.let { task ->
                _formState.update {
                    it.copy(
                        taskId = task.id,
                        name = task.name,
                        description = task.description,
                        scheduledDay = task.scheduledDay,
                        scheduledTime = task.scheduledTime,
                        recurrenceType = task.recurrenceType,
                        selectedDays = com.pixelquest.app.domain.WeeklyDays.effective(task.weeklyDays, task.scheduledDay),
                        category = task.category,
                        reminderEnabled = task.reminderEnabled,
                        reminderLeadMinutes = task.reminderLeadMinutes,
                        reminderStyle = task.reminderStyle,
                        createdAt = task.createdAt,
                        isActive = task.isActive,
                        isEditMode = true,
                        startsTomorrow = false
                    )
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
            withStart(state.copy(scheduledTime = time, timeError = error))
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

    fun onReminderEnabledChanged(enabled: Boolean) {
        _formState.update { it.copy(reminderEnabled = enabled) }
    }

    fun onReminderLeadSelected(minutes: Int) {
        _formState.update { it.copy(reminderLeadMinutes = minutes.coerceIn(0, 120)) }
    }

    fun onReminderStyleSelected(style: ReminderStyle) {
        _formState.update { it.copy(reminderStyle = style) }
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
            val time = state.scheduledTime ?: LocalTime.of(9, 0)
            val task = TaskEntity(
                id = state.taskId ?: 0,
                name = state.name.trim(),
                description = state.description,
                // A new quest whose time has passed today starts tomorrow; an edit keeps its start day.
                scheduledDay = if (state.isEditMode) state.scheduledDay else QuestStart.firstDay(appClock.now(), time),
                scheduledTime = time,
                recurrenceType = state.recurrenceType,
                category = state.category,
                isActive = state.isActive,
                createdAt = state.createdAt ?: System.currentTimeMillis(),
                reminderEnabled = state.reminderEnabled,
                reminderLeadMinutes = state.reminderLeadMinutes,
                reminderStyle = state.reminderStyle,
                weeklyDays = if (state.recurrenceType == RecurrenceType.WEEKLY) state.selectedDays else emptySet()
            )
            if (state.isEditMode && state.taskId != null && state.taskId > 0) {
                taskAlarmScheduler.cancelAlarmForTask(task)
                // A changed time may need today's reminder after all; a quest already done today
                // stays quiet (the receiver checks today's result).
                taskAlarmScheduler.forgetHandled(task.id)
                taskRepository.updateTask(task)
                taskAlarmScheduler.scheduleExactAlarmForTask(task)
            } else {
                val insertedId = taskRepository.insertTask(task)
                taskAlarmScheduler.scheduleExactAlarmForTask(task.copy(id = insertedId))
            }
            _formState.update { it.copy(isSubmitting = false, isSaveSuccess = true) }
            onSuccess()
        }
    }

    fun deleteTask(onSuccess: () -> Unit) {
        val state = _formState.value
        val id = state.taskId ?: return
        viewModelScope.launch {
            val task = TaskEntity(
                id = id,
                name = state.name,
                description = state.description,
                scheduledDay = state.scheduledDay,
                scheduledTime = state.scheduledTime ?: LocalTime.of(9, 0),
                recurrenceType = state.recurrenceType,
                category = state.category
            )
            taskAlarmScheduler.cancelAlarmForTask(task)
            // Its reminder or missed notice in the shade, and any pending snooze, go too.
            taskAlarmScheduler.clearReminder(id)
            taskRepository.deleteTask(task)
            onSuccess()
        }
    }
}
