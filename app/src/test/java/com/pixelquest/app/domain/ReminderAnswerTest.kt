package com.pixelquest.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** An answer from a reminder belongs to the day of the quest the reminder was for. */
class ReminderAnswerTest {

    private val today = LocalDate.of(2026, 10, 8)

    @Test
    fun yesAfterMidnight_creditsTheQuestsDay_notTheNewOne() {
        // A 23:00 quest from yesterday, answered at 00:15.
        assertEquals(today.minusDays(1), ReminderAnswer.dateToRecord(today.minusDays(1), today))
    }

    @Test
    fun anEveningBeforeReminder_creditsTomorrowsQuest() {
        assertEquals(today.plusDays(1), ReminderAnswer.dateToRecord(today.plusDays(1), today))
    }

    @Test
    fun anOldReminder_isTooLateToRecord() {
        assertNull(ReminderAnswer.dateToRecord(today.minusDays(2), today))
    }

    @Test
    fun aReminderWithoutADay_meansToday() {
        assertEquals(today, ReminderAnswer.dateToRecord(null, today))
    }
}
