package com.pixelquest.app.ui.screens.tasks

import com.pixelquest.app.domain.model.RecurrenceType
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
    val selectedDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    ),
    val category: TaskCategory = TaskCategory.FITNESS,
    val nameError: String? = null,
    val timeError: String? = null,
    val daysError: String? = null,
    val isEditMode: Boolean = false,
    val taskId: Long? = null,
    val isSubmitting: Boolean = false,
    val isSaveSuccess: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && scheduledTime != null && (recurrenceType != RecurrenceType.WEEKLY || selectedDays.isNotEmpty())
}
