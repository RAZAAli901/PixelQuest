package com.pixelquest.app.domain

import com.pixelquest.app.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Whether a task is due on a given date. A task's scheduledDay is its first occurrence and is never
 * advanced, so recurring tasks are matched by interval from that date.
 */
object TaskOccurrence {

    fun occursOn(
        scheduledDay: LocalDate,
        recurrence: RecurrenceType,
        date: LocalDate,
        weeklyDays: Set<DayOfWeek> = emptySet()
    ): Boolean {
        if (date.isBefore(scheduledDay)) return false
        return when (recurrence) {
            RecurrenceType.ONE_TIME -> date == scheduledDay
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> date.dayOfWeek in WeeklyDays.effective(weeklyDays, scheduledDay)
            // plusMonths clamps (Jan 31 -> Feb 28), matching how ReminderSchedule steps months.
            RecurrenceType.MONTHLY -> {
                val months = ChronoUnit.MONTHS.between(scheduledDay.withDayOfMonth(1), date.withDayOfMonth(1))
                scheduledDay.plusMonths(months) == date
            }
        }
    }
}
