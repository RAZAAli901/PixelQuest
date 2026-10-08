package com.pixelquest.app.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The nightly streak check runs at 02:05 local time: after the 2-hour window of a quest as late as
 * 23:59 (it ran at 00:05, settling yesterday before a 23:00 quest's window closed), and computed in
 * the time zone so a clock change doesn't move it (the 24-hour periodic job drifted to 23:05).
 */
class NightlyStreakScheduleTest {

    private val london = ZoneId.of("Europe/London")

    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), london)

    @Test
    fun beforeTwoAm_itRunsTonight() {
        assertEquals(Duration.ofMinutes(3 * 60 + 5), StreakEvaluationWorker.delayUntilNextRun(at("2026-10-08T23:00")))
        assertEquals(Duration.ofMinutes(65), StreakEvaluationWorker.delayUntilNextRun(at("2026-10-09T01:00")))
    }

    @Test
    fun afterTwoAm_itRunsTomorrowNight() {
        assertEquals(Duration.ofHours(24), StreakEvaluationWorker.delayUntilNextRun(at("2026-10-09T02:05")))
        assertEquals(Duration.ofMinutes(23 * 60 + 5), StreakEvaluationWorker.delayUntilNextRun(at("2026-10-09T03:00")))
    }

    @Test
    fun onTheNightTheClocksGoBack_itStillRunsAt0205LocalTime() {
        // 25 Oct 2026: 02:00 BST becomes 01:00 GMT, so the night is an hour longer.
        val delay = StreakEvaluationWorker.delayUntilNextRun(at("2026-10-24T23:00"))

        assertEquals(Duration.ofMinutes(4 * 60 + 5), delay)
        assertEquals(java.time.LocalTime.of(2, 5), at("2026-10-24T23:00").plus(delay).withZoneSameInstant(london).toLocalTime())
    }

    @Test
    fun onTheNightTheClocksGoForward_itStillRunsAt0205LocalTime() {
        // 28 Mar 2027: 01:00 GMT becomes 02:00 BST, so the night is an hour shorter.
        val delay = StreakEvaluationWorker.delayUntilNextRun(at("2027-03-27T23:00"))

        assertEquals(Duration.ofMinutes(2 * 60 + 5), delay)
    }
}
