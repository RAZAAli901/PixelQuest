package com.pixelquest.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The weekdays a weekly task repeats on. Stored as a 7-bit mask in tasks.weeklyDays, Monday in
 * bit 0 through Sunday in bit 6 (bit = DayOfWeek.value - 1).
 *
 * An empty set means the task was saved before the day picker was stored, so it keeps repeating
 * on its first date's weekday.
 */
object WeeklyDays {

    fun toMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun fromMask(mask: Int): Set<DayOfWeek> = DayOfWeek.values().filterTo(sortedSetOf()) { mask and (1 shl (it.value - 1)) != 0 }

    /** The days a weekly task actually repeats on. */
    fun effective(days: Set<DayOfWeek>, scheduledDay: LocalDate): Set<DayOfWeek> = days.ifEmpty { setOf(scheduledDay.dayOfWeek) }
}
