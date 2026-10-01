package com.pixelquest.app.scheduling

import com.pixelquest.app.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduleTest {

    private val eight = LocalTime.of(8, 0)

    @Test
    fun daily_laterToday_firesToday() {
        val now = LocalDateTime.of(2026, 10, 2, 7, 0)
        val next = ReminderSchedule.nextTriggerAt(LocalDate.of(2026, 10, 2), eight, RecurrenceType.DAILY, now)
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 0), next)
    }

    @Test
    fun daily_storedDateLongAgo_firesNextFutureSlot() {
        // Regression: the old calculation only added one day, so boot re-arm set alarms in the past.
        val now = LocalDateTime.of(2026, 10, 2, 9, 0)
        val next = ReminderSchedule.nextTriggerAt(LocalDate.of(2026, 8, 1), eight, RecurrenceType.DAILY, now)
        assertEquals(LocalDateTime.of(2026, 10, 3, 8, 0), next)
    }

    @Test
    fun daily_completedToday_skipsToTomorrow() {
        val now = LocalDateTime.of(2026, 10, 2, 7, 0)
        val next = ReminderSchedule.nextTriggerAt(
            LocalDate.of(2026, 9, 1), eight, RecurrenceType.DAILY, now,
            notBefore = LocalDate.of(2026, 10, 3)
        )
        assertEquals(LocalDateTime.of(2026, 10, 3, 8, 0), next)
    }

    @Test
    fun weekly_keepsWeekday() {
        val now = LocalDateTime.of(2026, 10, 2, 12, 0) // Friday
        val next = ReminderSchedule.nextTriggerAt(LocalDate.of(2026, 9, 7), eight, RecurrenceType.WEEKLY, now) // Monday
        assertEquals(LocalDateTime.of(2026, 10, 5, 8, 0), next)
    }

    @Test
    fun monthly_31st_doesNotDrift() {
        val now = LocalDateTime.of(2026, 3, 1, 0, 0)
        val next = ReminderSchedule.nextTriggerAt(LocalDate.of(2026, 1, 31), eight, RecurrenceType.MONTHLY, now)
        assertEquals(LocalDateTime.of(2026, 3, 31, 8, 0), next)
    }

    @Test
    fun oneTime_inPast_returnsNull() {
        val now = LocalDateTime.of(2026, 10, 2, 9, 0)
        assertNull(ReminderSchedule.nextTriggerAt(LocalDate.of(2026, 10, 2), eight, RecurrenceType.ONE_TIME, now))
    }

    @Test
    fun oneTime_inFuture_returnsIt() {
        val now = LocalDateTime.of(2026, 10, 2, 7, 0)
        val next = ReminderSchedule.nextTriggerAt(LocalDate.of(2026, 10, 2), eight, RecurrenceType.ONE_TIME, now)
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 0), next)
    }

    @Test
    fun leadMinutes_firesEarly() {
        val now = LocalDateTime.of(2026, 10, 2, 7, 0)
        val next = ReminderSchedule.nextTriggerAt(
            LocalDate.of(2026, 10, 2), eight, RecurrenceType.DAILY, now, leadMinutes = 15
        )
        assertEquals(LocalDateTime.of(2026, 10, 2, 7, 45), next)
    }

    @Test
    fun leadMinutes_alreadyInsideLeadWindow_firesAtTaskTime() {
        val now = LocalDateTime.of(2026, 10, 2, 7, 50)
        val next = ReminderSchedule.nextTriggerAt(
            LocalDate.of(2026, 10, 2), eight, RecurrenceType.DAILY, now, leadMinutes = 15
        )
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 0), next)
    }
}
