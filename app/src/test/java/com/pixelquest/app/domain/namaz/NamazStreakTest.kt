package com.pixelquest.app.domain.namaz

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Days in a row with all five prayers prayed. */
class NamazStreakTest {

    private val today = LocalDate.of(2026, 10, 17)
    private fun daysBack(vararg prayed: Int) = prayed.withIndex().associate { (i, n) -> today.minusDays(i.toLong()) to n }

    @Test
    fun noPrayers_noStreak() {
        assertEquals(0, NamazStreak.current(emptyMap(), today))
    }

    @Test
    fun todayCounts_onceAllFiveArePrayed() {
        assertEquals(3, NamazStreak.current(daysBack(5, 5, 5, 2), today))
    }

    @Test
    fun anUnfinishedToday_keepsTheRunUpToYesterday() {
        // Today has 3 so far: the streak is still yesterday's 2.
        assertEquals(2, NamazStreak.current(daysBack(3, 5, 5, 4), today))
    }

    @Test
    fun aDayWithFewerThanFive_endsTheRun() {
        assertEquals(1, NamazStreak.current(daysBack(5, 4, 5, 5), today))
        assertEquals(0, NamazStreak.current(daysBack(0, 4, 5, 5), today))
    }

    @Test
    fun aDayWithNoRecord_endsTheRunToo() {
        val prayed = mapOf(today to 5, today.minusDays(2) to 5)
        assertEquals(1, NamazStreak.current(prayed, today))
    }
}
