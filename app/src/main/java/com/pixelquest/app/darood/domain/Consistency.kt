package com.pixelquest.app.darood.domain

import java.time.LocalDate

/**
 * How steady a member is in a party, from the days they posted on (each in their own time zone).
 *
 * @param activeLast30 days with an entry in the last 30, today included.
 * @param currentStreak days in a row with an entry, up to today. A streak that ran to yesterday is
 *   still current, since today isn't over.
 * @param bestStreak the longest run of days in a row, ever.
 * @param averagePerActiveDay what they add on a day they post, on average (rounded).
 * @param last30 the days of the last 30 that have an entry, for the calendar.
 */
data class Consistency(
    val activeLast30: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val averagePerActiveDay: Long,
    val last30: Set<LocalDate>
) {
    companion object {
        /** The window the calendar and active days look back over. */
        const val WINDOW_DAYS = 30L

        /** From each day's total ([days]; days without entries are left out), as of [today]. */
        fun of(days: Map<LocalDate, Long>, today: LocalDate): Consistency {
            val active = days.filterValues { it > 0 }.keys
            val windowStart = today.minusDays(WINDOW_DAYS - 1)
            val last30 = active.filter { !it.isBefore(windowStart) && !it.isAfter(today) }.toSet()

            // Current: back from today, or from yesterday if today has nothing yet.
            var day = if (today in active) today else today.minusDays(1)
            var current = 0
            while (day in active) {
                current++
                day = day.minusDays(1)
            }

            var best = 0
            var run = 0
            var previous: LocalDate? = null
            for (d in active.sorted()) {
                run = if (previous != null && d == previous.plusDays(1)) run + 1 else 1
                best = maxOf(best, run)
                previous = d
            }

            val total = days.values.filter { it > 0 }.sum()
            val average = if (active.isEmpty()) 0 else Math.round(total.toDouble() / active.size)
            return Consistency(last30.size, current, best, average, last30)
        }
    }
}
