package com.pixelquest.app.domain

import java.time.LocalDate

/**
 * Which day an answer from a reminder ("Yes, I did it", "Not yet", or the full-screen prompt)
 * belongs to: the day of the quest occurrence the reminder was for, not the day it was tapped. A
 * "Yes" at 00:15 for a 23:00 quest used to credit the new day, leaving the real day to be marked
 * missed. Reminders from before Day 31 carry no day, so they fall back to today. An answer for a day
 * before yesterday is too late to record: that day's result and streak are settled.
 */
object ReminderAnswer {
    fun dateToRecord(occurrenceDate: LocalDate?, today: LocalDate): LocalDate? {
        val day = occurrenceDate ?: today
        return day.takeIf { !it.isBefore(today.minusDays(1)) }
    }
}
