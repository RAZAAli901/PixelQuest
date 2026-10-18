package com.pixelquest.app.ui.screens.namaz

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import java.time.LocalDate

/**
 * A date in the Hijri calendar, by Umm al-Qura (Saudi Arabia's official calendar).
 *
 * @param month 1 (Muharram) to 12 (Dhu al-Hijjah).
 */
data class HijriDate(val day: Int, val month: Int, val year: Int) {
    companion object {
        /**
         * [date] in the Hijri calendar. Android's own ICU calendar, which works from Android 7;
         * java.time's HijrahDate needs Android 8. The date follows the Gregorian [date] whole,
         * though the Hijri day begins at Maghrib: the simplest reading, close enough for a line
         * under the title.
         */
        fun of(date: LocalDate): HijriDate {
            val utc = TimeZone.getTimeZone("UTC")
            val calendar = IslamicCalendar(utc).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                // Noon UTC on the day, so no time zone moves it to a neighbouring date.
                timeInMillis = date.atTime(12, 0).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            }
            return HijriDate(
                day = calendar.get(IslamicCalendar.DAY_OF_MONTH),
                month = calendar.get(IslamicCalendar.MONTH) + 1,
                year = calendar.get(IslamicCalendar.YEAR)
            )
        }
    }
}
