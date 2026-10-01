package com.pixelquest.app.domain

import com.pixelquest.app.domain.model.RecurrenceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskOccurrenceTest {

    private val start = LocalDate.of(2026, 10, 1) // Thursday

    @Test
    fun daily_dueEveryDayFromStart() {
        assertTrue(TaskOccurrence.occursOn(start, RecurrenceType.DAILY, start))
        assertTrue(TaskOccurrence.occursOn(start, RecurrenceType.DAILY, start.plusDays(1)))
        assertTrue(TaskOccurrence.occursOn(start, RecurrenceType.DAILY, start.plusDays(400)))
        assertFalse(TaskOccurrence.occursOn(start, RecurrenceType.DAILY, start.minusDays(1)))
    }

    @Test
    fun weekly_dueOnSameWeekdayOnly() {
        assertTrue(TaskOccurrence.occursOn(start, RecurrenceType.WEEKLY, start.plusWeeks(3)))
        assertFalse(TaskOccurrence.occursOn(start, RecurrenceType.WEEKLY, start.plusDays(1)))
    }

    @Test
    fun monthly_dueOnSameDate_andClampsShortMonths() {
        val jan31 = LocalDate.of(2026, 1, 31)
        assertTrue(TaskOccurrence.occursOn(jan31, RecurrenceType.MONTHLY, LocalDate.of(2026, 2, 28)))
        assertTrue(TaskOccurrence.occursOn(jan31, RecurrenceType.MONTHLY, LocalDate.of(2026, 3, 31)))
        assertFalse(TaskOccurrence.occursOn(jan31, RecurrenceType.MONTHLY, LocalDate.of(2026, 3, 28)))
    }

    @Test
    fun oneTime_dueOnlyOnItsDay() {
        assertTrue(TaskOccurrence.occursOn(start, RecurrenceType.ONE_TIME, start))
        assertFalse(TaskOccurrence.occursOn(start, RecurrenceType.ONE_TIME, start.plusDays(1)))
    }
}
