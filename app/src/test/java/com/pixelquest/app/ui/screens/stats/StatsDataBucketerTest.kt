package com.pixelquest.app.ui.screens.stats

import com.pixelquest.app.domain.model.DailyStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsDataBucketerTest {

    private val today = LocalDate.of(2026, 8, 25)

    @Test
    fun calculateWeeklyBuckets_returnsCorrectNumberOfWeeks() {
        val sampleMap = mapOf(
            today to DailyStatus.PERFECT,
            today.minusDays(1) to DailyStatus.PERFECT,
            today.minusDays(7) to DailyStatus.MISSED
        )

        val buckets = StatsDataBucketer.calculateWeeklyBuckets(sampleMap, today, weeksCount = 4)
        assertEquals(4, buckets.size)
        assertEquals("W-3", buckets[0].first)
        assertEquals("NOW", buckets[3].first)
    }

    @Test
    fun calculateWeeklyBuckets_allPerfectDays_returnsOneHundredPercentForActiveWeek() {
        val sampleMap = (0..6).associate { daysAgo ->
            today.minusDays(daysAgo.toLong()) to DailyStatus.PERFECT
        }

        val buckets = StatsDataBucketer.calculateWeeklyBuckets(sampleMap, today, weeksCount = 4)
        val currentWeekRate = buckets.last().second
        assertEquals(1.0f, currentWeekRate, 0.001f)
    }

    @Test
    fun calculateWeeklyBuckets_emptyMap_returnsZeroRates() {
        val buckets = StatsDataBucketer.calculateWeeklyBuckets(emptyMap(), today, weeksCount = 4)
        buckets.forEach { (_, rate) ->
            assertEquals(0.0f, rate, 0.001f)
        }
    }
}
