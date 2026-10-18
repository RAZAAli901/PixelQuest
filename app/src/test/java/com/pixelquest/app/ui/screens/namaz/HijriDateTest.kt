package com.pixelquest.app.ui.screens.namaz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Gregorian dates to Hijri ones, by Umm al-Qura. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class HijriDateTest {

    @Test
    fun theFirstOfRamadan1447_isOnTheDayItBegan() {
        // Umm al-Qura's 1 Ramadan 1447 is Wednesday 18 February 2026.
        assertEquals(HijriDate(1, 9, 1447), HijriDate.of(LocalDate.of(2026, 2, 18)))
        assertEquals(HijriDate(29, 8, 1447), HijriDate.of(LocalDate.of(2026, 2, 17)))
    }

    @Test
    fun mid_October2026_isInJumadaAlUla1448() {
        val hijri = HijriDate.of(LocalDate.of(2026, 10, 17))
        assertEquals(1448, hijri.year)
        assertEquals(5, hijri.month)
        assertTrue(hijri.day in 4..8)
    }

    @Test
    fun consecutiveDays_areConsecutive() {
        var date = LocalDate.of(2026, 1, 1)
        var previous = HijriDate.of(date)
        repeat(400) {
            date = date.plusDays(1)
            val next = HijriDate.of(date)
            val sameMonth = next.month == previous.month && next.year == previous.year
            assertTrue("$date: $previous then $next", if (sameMonth) next.day == previous.day + 1 else next.day == 1)
            previous = next
        }
    }
}
