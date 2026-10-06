package com.pixelquest.app.ui.screens.tasks

import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class TaskFormState(
    val name: String = "",
    val description: String = "",
    val scheduledDay: LocalDate = LocalDate.now(),
    val scheduledTime: LocalTime? = LocalTime.of(9, 0),
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    // A new weekly quest starts on today's weekday; the picker adds or removes days from there.
    val selectedDays: Set<DayOfWeek> = setOf(scheduledDay.dayOfWeek),
    val category: TaskCategory = TaskCategory.FITNESS,
    val reminderEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val reminderStyle: ReminderStyle = ReminderStyle.STANDARD,
    /** Kept from the stored task so an edit doesn't reset them. */
    val createdAt: Long? = null,
    val isActive: Boolean = true,
    val nameError: String? = null,
    val timeError: String? = null,
    val daysError: String? = null,
    val isEditMode: Boolean = false,
    val taskId: Long? = null,
    val isSubmitting: Boolean = false,
    val isSaveSuccess: Boolean = false,
    /** For a new quest: its time has already passed today, so it starts tomorrow (see QuestStart). */
    val startsTomorrow: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && scheduledTime != null && (recurrenceType != RecurrenceType.WEEKLY || selectedDays.isNotEmpty())
}

/** Lead times offered in the reminder section, in minutes before the task. */
val REMINDER_LEAD_OPTIONS = listOf(0, 5, 15, 30, 60)
