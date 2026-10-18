package com.pixelquest.app.domain.namaz

import java.time.LocalDate

/** The Namaz streak: prayer days in a row with all five prayers prayed (docs/namaz-design.md). */
object NamazStreak {

    const val PRAYERS_PER_DAY = 5

    /**
     * Days in a row with all five prayed, up to [today]. Today counts once its five are prayed;
     * until then, the run up to yesterday still stands, so a streak doesn't drop to 0 at Fajr.
     *
     * @param prayedPerDay prayers prayed per prayer day; a day missing from it had none.
     */
    fun current(prayedPerDay: Map<LocalDate, Int>, today: LocalDate): Int {
        fun complete(day: LocalDate) = (prayedPerDay[day] ?: 0) >= PRAYERS_PER_DAY
        var day = if (complete(today)) today else today.minusDays(1)
        var count = 0
        while (complete(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}
