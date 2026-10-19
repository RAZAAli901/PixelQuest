package com.pixelquest.app.darood.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** How steady a member is: active days, the streak now, the best, and their average. */
class ConsistencyTest {

    private val today = LocalDate.of(2026, 10, 20)

    private fun daysBack(vararg back: Long, each: Long = 100) = back.associate { today.minusDays(it) to each }

    @Test
    fun aStreakToToday_counts_andSoDoesOneToYesterday_todayNotBeingOver() {
        assertEquals(3, Consistency.of(daysBack(0, 1, 2, 5), today).currentStreak)
        assertEquals("today not posted yet: still current", 2, Consistency.of(daysBack(1, 2, 5), today).currentStreak)
        assertEquals("a day missed: over", 0, Consistency.of(daysBack(2, 3, 4), today).currentStreak)
        assertEquals(0, Consistency.of(emptyMap(), today).currentStreak)
    }

    @Test
    fun theBestRun_isTheLongestEver_evenLongAgo() {
        val old = (100L..106L).toList().toLongArray()
        assertEquals(7, Consistency.of(daysBack(0, 1, *old), today).bestStreak)
        assertEquals(2, Consistency.of(daysBack(0, 1), today).bestStreak)
        assertEquals(1, Consistency.of(daysBack(0, 2, 4), today).bestStreak)
    }

    @Test
    fun activeDays_lookBack30_todayIncluded() {
        val c = Consistency.of(daysBack(0, 1, 29, 30, 45), today)
        assertEquals("today, yesterday and 29 days back; not 30 or 45", 3, c.activeLast30)
        assertEquals(setOf(today, today.minusDays(1), today.minusDays(29)), c.last30)
        assertEquals("a future day (another time zone ahead) doesn't count", 0, Consistency.of(daysBack(-1), today).activeLast30)
    }

    @Test
    fun theAverage_isPerDayPosted_rounded() {
        val days = mapOf(today to 300L, today.minusDays(1) to 13L, today.minusDays(9) to 1_000L)
        assertEquals(Math.round(1_313.0 / 3), Consistency.of(days, today).averagePerActiveDay)
        assertEquals(0, Consistency.of(emptyMap(), today).averagePerActiveDay)
        assertEquals("a day of nothing isn't a day posted", 100, Consistency.of(mapOf(today to 100L, today.minusDays(1) to 0L), today).averagePerActiveDay)
    }
}
