package com.pixelquest.app.scheduling

import com.pixelquest.app.domain.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure calculation of when a task's next reminder should fire.
 *
 * The stored scheduledDay is the task's first occurrence and is not advanced in the database,
 * so the next trigger is found by stepping forward from it by the recurrence interval until it
 * lands after [now] (and on or after [notBefore], used after a task is completed or skipped today).
 */
object ReminderSchedule {

    private const val MAX_STEPS = 10_000

    /**
     * Returns the next reminder time, or null when the task has no future occurrence
     * (a one-time task whose time has passed).
     */
    fun nextTriggerAt(
        scheduledDay: LocalDate,
        scheduledTime: LocalTime,
        recurrence: RecurrenceType,
        now: LocalDateTime,
        leadMinutes: Int = 0,
        notBefore: LocalDate? = null
    ): LocalDateTime? {
        val lead = leadMinutes.coerceAtLeast(0).toLong()
        val base = LocalDateTime.of(scheduledDay, scheduledTime)

        if (recurrence == RecurrenceType.ONE_TIME) {
            val dayAllowed = notBefore == null || !scheduledDay.isBefore(notBefore)
            return if (dayAllowed) triggerFor(base, lead, now) else null
        }

        var step = 0L
        while (step < MAX_STEPS) {
            val occurrence = when (recurrence) {
                RecurrenceType.DAILY -> base.plusDays(step)
                RecurrenceType.WEEKLY -> base.plusWeeks(step)
                // Step from the original date each time so a 31st does not drift to the 28th.
                RecurrenceType.MONTHLY -> base.plusMonths(step)
                RecurrenceType.ONE_TIME -> base
            }
            val dayAllowed = notBefore == null || !occurrence.toLocalDate().isBefore(notBefore)
            val trigger = if (dayAllowed) triggerFor(occurrence, lead, now) else null
            if (trigger != null) return trigger
            step++
        }
        return null
    }

    /**
     * Lead-adjusted time for one occurrence. If the lead window has already started but the task
     * itself is still ahead, remind at the task time instead of dropping today's reminder.
     */
    private fun triggerFor(occurrence: LocalDateTime, lead: Long, now: LocalDateTime): LocalDateTime? {
        val early = occurrence.minusMinutes(lead)
        return when {
            early.isAfter(now) -> early
            occurrence.isAfter(now) -> occurrence
            else -> null
        }
    }
}
