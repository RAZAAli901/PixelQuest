package com.pixelquest.app.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskEntity
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
    private val savedStateHandle: SavedStateHandle,
    private val appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock(),
) : ViewModel() {

    constructor(
        taskRepository: TaskRepository,
        taskAlarmScheduler: TaskAlarmScheduler,
        appClock: com.pixelquest.app.util.AppClock = com.pixelquest.app.util.AppClock(),
    ) : this(taskRepository, taskAlarmScheduler, SavedStateHandle(), appClock)

    companion object {
        private const val KEY_TASK_ID = "task_form_task_id"
        private const val KEY_NAME = "task_form_name"
        private const val KEY_DESCRIPTION = "task_form_description"
        private const val KEY_HOUR = "task_form_time_hour"
        private const val KEY_MINUTE = "task_form_time_minute"
        private const val KEY_RECURRENCE = "task_form_recurrence"
        private const val KEY_SELECTED_DAYS = "task_form_selected_days"
        private const val KEY_CATEGORY = "task_form_category"
        private const val KEY_REMINDER_ENABLED = "task_form_reminder_enabled"
        private const val KEY_REMINDER_LEAD = "task_form_reminder_lead"
        private const val KEY_REMINDER_STYLE = "task_form_reminder_style"
        private const val KEY_IS_EDIT = "task_form_is_edit"
    }

    private val _formState = MutableStateFlow(
        run {
            val initial = TaskFormState()
            val savedName = savedStateHandle.get<String>(KEY_NAME)
            if (savedName != null) {
                val hour = savedStateHandle.get<Int>(KEY_HOUR)
                val minute = savedStateHandle.get<Int>(KEY_MINUTE)
                val time = if (hour != null && minute != null) LocalTime.of(hour, minute) else initial.scheduledTime
                val recurrence = savedStateHandle.get<String>(KEY_RECURRENCE)?.let { runCatching { RecurrenceType.valueOf(it) }.getOrNull() } ?: initial.recurrenceType
                val days = savedStateHandle.get<List<String>>(KEY_SELECTED_DAYS)?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }?.toSet() ?: initial.selectedDays
                val category = savedStateHandle.get<String>(KEY_CATEGORY)?.let { runCatching { TaskCategory.valueOf(it) }.getOrNull() } ?: initial.category
                val reminderStyle = savedStateHandle.get<String>(KEY_REMINDER_STYLE)?.let { runCatching { ReminderStyle.valueOf(it) }.getOrNull() } ?: initial.reminderStyle

                withStart(
                    initial.copy(
                        taskId = savedStateHandle.get<Long>(KEY_TASK_ID),
                        name = savedName,
                        description = savedStateHandle.get<String>(KEY_DESCRIPTION) ?: "",
                        scheduledTime = time,
                        recurrenceType = recurrence,
                        selectedDays = days,
                        category = category,
                        reminderEnabled = savedStateHandle.get<Boolean>(KEY_REMINDER_ENABLED) ?: initial.reminderEnabled,
                        reminderLeadMinutes = savedStateHandle.get<Int>(KEY_REMINDER_LEAD) ?: initial.reminderLeadMinutes,
                        reminderStyle = reminderStyle,
                        isEditMode = savedStateHandle.get<Boolean>(KEY_IS_EDIT) ?: false,
                    ),
                )
            } else {
                withStart(initial)
            }
        },
    )

    /** Whether Android lets reminders fire at their exact time ("Alarms & reminders"). */
    fun canScheduleExactAlarms(): Boolean = taskAlarmScheduler.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): android.content.Intent = taskAlarmScheduler.openExactAlarmSettingsIntent()

    /** New quests start today so they show up on the Home tab until completed; edited quests keep their start day. */
    private fun withStart(state: TaskFormState): TaskFormState = state.copy(startsTomorrow = false)
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    private var loadedTaskId: Long? = savedStateHandle.get<Long>(KEY_TASK_ID)

    /**
     * Fills the form from the stored task once. Calling it again for the same task (the screen does
     * after a rotation) keeps the player's unsaved edits, and later database changes don't overwrite them.
     */
    fun loadTask(taskId: Long) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId
        savedStateHandle[KEY_TASK_ID] = taskId
        viewModelScope.launch {
            taskRepository.getTaskById(taskId).first()?.let { task ->
                val days = com.pixelquest.app.domain.WeeklyDays.effective(task.weeklyDays, task.scheduledDay)
                savedStateHandle[KEY_NAME] = task.name
                savedStateHandle[KEY_DESCRIPTION] = task.description
                savedStateHandle[KEY_HOUR] = task.scheduledTime?.hour
                savedStateHandle[KEY_MINUTE] = task.scheduledTime?.minute
                savedStateHandle[KEY_RECURRENCE] = task.recurrenceType.name
                savedStateHandle[KEY_SELECTED_DAYS] = days.map { it.name }
                savedStateHandle[KEY_CATEGORY] = task.category.name
                savedStateHandle[KEY_REMINDER_ENABLED] = task.reminderEnabled
                savedStateHandle[KEY_REMINDER_LEAD] = task.reminderLeadMinutes
                savedStateHandle[KEY_REMINDER_STYLE] = task.reminderStyle.name
                savedStateHandle[KEY_IS_EDIT] = true

                _formState.update {
                    it.copy(
                        taskId = task.id,
                        name = task.name,
                        description = task.description,
                        scheduledDay = task.scheduledDay,
                        scheduledTime = task.scheduledTime,
                        recurrenceType = task.recurrenceType,
                        selectedDays = days,
                        category = task.category,
                        reminderEnabled = task.reminderEnabled,
                        reminderLeadMinutes = task.reminderLeadMinutes,
                        reminderStyle = task.reminderStyle,
                        createdAt = task.createdAt,
                        isActive = task.isActive,
                        isEditMode = true,
                        startsTomorrow = false,
                    )
                }
            }
        }
    }

    fun onNameChanged(raw: String) {
        // One line, no control characters, at most 100 characters (S053).
        val name = com.pixelquest.app.domain.validation.InputRules.typed(raw, com.pixelquest.app.domain.validation.InputRules.Field.QUEST_NAME)
        savedStateHandle[KEY_NAME] = name
        _formState.update { state ->
            val error = if (name.isBlank()) "Quest title is required!" else null
            state.copy(name = name, nameError = error)
        }
    }

    fun onDayToggled(day: DayOfWeek) {
        _formState.update { state ->
            val current = state.selectedDays
            val updated = if (current.contains(day)) current - day else current + day
            savedStateHandle[KEY_SELECTED_DAYS] = updated.map { it.name }
            val error = if (state.recurrenceType == RecurrenceType.WEEKLY && updated.isEmpty()) {
                "Select at least one day!"
            } else {
                null
            }
            state.copy(selectedDays = updated, daysError = error)
        }
    }

    fun onTimeSelected(time: LocalTime?) {
        savedStateHandle[KEY_HOUR] = time?.hour
        savedStateHandle[KEY_MINUTE] = time?.minute
        _formState.update { state ->
            val error = if (time == null) "Time is required!" else null
            withStart(state.copy(scheduledTime = time, timeError = error))
        }
    }

    fun onRecurrenceSelected(recurrence: RecurrenceType) {
        savedStateHandle[KEY_RECURRENCE] = recurrence.name
        _formState.update { state ->
            val daysErr = if (recurrence == RecurrenceType.WEEKLY && state.selectedDays.isEmpty()) {
                "Select at least one day!"
            } else {
                null
            }
            state.copy(recurrenceType = recurrence, daysError = daysErr)
        }
    }

    fun onCategorySelected(category: TaskCategory) {
        savedStateHandle[KEY_CATEGORY] = category.name
        _formState.update { it.copy(category = category) }
    }

    fun onReminderEnabledChanged(enabled: Boolean) {
        savedStateHandle[KEY_REMINDER_ENABLED] = enabled
        _formState.update { it.copy(reminderEnabled = enabled) }
    }

    fun onReminderLeadSelected(minutes: Int) {
        val lead = minutes.coerceIn(0, 120)
        savedStateHandle[KEY_REMINDER_LEAD] = lead
        _formState.update { it.copy(reminderLeadMinutes = lead) }
    }

    fun onReminderStyleSelected(style: ReminderStyle) {
        savedStateHandle[KEY_REMINDER_STYLE] = style.name
        _formState.update { it.copy(reminderStyle = style) }
    }

    fun validateForm(): Boolean {
        val state = _formState.value
        val nameErr = if (state.name.isBlank()) "Quest title is required!" else null
        val timeErr = if (state.scheduledTime == null) "Time is required!" else null
        val daysErr = if (state.recurrenceType == RecurrenceType.WEEKLY && state.selectedDays.isEmpty()) {
            "Select at least one day!"
        } else {
            null
        }

        _formState.update {
            it.copy(
                nameError = nameErr,
                timeError = timeErr,
                daysError = daysErr,
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
                // A new quest starts today so it shows up on the Home tab until completed; an edit keeps its start day.
                scheduledDay = if (state.isEditMode) state.scheduledDay else appClock.now().toLocalDate(),
                scheduledTime = time,
                recurrenceType = state.recurrenceType,
                category = state.category,
                isActive = state.isActive,
                createdAt = state.createdAt ?: System.currentTimeMillis(),
                reminderEnabled = state.reminderEnabled,
                reminderLeadMinutes = state.reminderLeadMinutes,
                reminderStyle = state.reminderStyle,
                weeklyDays = if (state.recurrenceType == RecurrenceType.WEEKLY) state.selectedDays else emptySet(),
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
                category = state.category,
            )
            taskAlarmScheduler.cancelAlarmForTask(task)
            // Its reminder or missed notice in the shade, and any pending snooze, go too.
            taskAlarmScheduler.clearReminder(id)
            taskRepository.deleteTask(task)
            onSuccess()
        }
    }
}
