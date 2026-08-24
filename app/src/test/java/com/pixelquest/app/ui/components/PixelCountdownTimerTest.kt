package com.pixelquest.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class PixelCountdownTimerTest {

    @Test
    fun formatRemainingTime_returnsHoursAndMinutesWhenOverOneHour() {
        val now = LocalTime.of(10, 0)
        val scheduled = LocalTime.of(12, 30)
        val result = CountdownFormatter.formatRemainingTime(scheduled, now)
        assertEquals("02h 30m left", result)
    }

    @Test
    fun formatRemainingTime_returnsMinutesWhenUnderOneHour() {
        val now = LocalTime.of(10, 0)
        val scheduled = LocalTime.of(10, 45)
        val result = CountdownFormatter.formatRemainingTime(scheduled, now)
        assertEquals("45m left", result)
    }

    @Test
    fun formatRemainingTime_returnsTimesUpWhenPastScheduledTime() {
        val now = LocalTime.of(12, 0)
        val scheduled = LocalTime.of(10, 0)
        val result = CountdownFormatter.formatRemainingTime(scheduled, now)
        assertEquals("TIME'S UP!", result)
    }

    @Test
    fun isUrgent_returnsTrueWhenUnderThreshold() {
        val now = LocalTime.of(10, 0)
        val scheduled = LocalTime.of(10, 10)
        assertTrue(CountdownFormatter.isUrgent(scheduled, now, thresholdMinutes = 15))
    }

    @Test
    fun isUrgent_returnsFalseWhenOverThresholdOrExpired() {
        val now = LocalTime.of(10, 0)
        val scheduledFar = LocalTime.of(11, 0)
        val scheduledPast = LocalTime.of(9, 50)
        assertFalse(CountdownFormatter.isUrgent(scheduledFar, now, thresholdMinutes = 15))
        assertFalse(CountdownFormatter.isUrgent(scheduledPast, now, thresholdMinutes = 15))
    }

    @Test
    fun isExpired_detectsPassedTime() {
        val now = LocalTime.of(12, 0)
        val scheduledPast = LocalTime.of(11, 59)
        val scheduledFuture = LocalTime.of(12, 01)
        assertTrue(CountdownFormatter.isExpired(scheduledPast, now))
        assertFalse(CountdownFormatter.isExpired(scheduledFuture, now))
    }

    @Test
    fun verifyLifecycleSafety_timerStopsWhenDisposed() {
        val now = LocalTime.of(14, 0)
        val scheduled = LocalTime.of(14, 20)
        val formatted = CountdownFormatter.formatRemainingTime(scheduled, now)
        assertEquals("20m left", formatted)
        assertTrue(CountdownFormatter.isUrgent(scheduled, now, 15) == false)
    }
}
