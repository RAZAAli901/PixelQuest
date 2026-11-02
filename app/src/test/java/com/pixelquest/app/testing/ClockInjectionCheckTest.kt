package com.pixelquest.app.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.Description
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ClockInjectionCheckTest {

    private val baselineInstant = Instant.parse("2026-06-01T12:00:00Z")
    private val fakeClock = FakeClock(baselineInstant, ZoneId.of("UTC"))

    @Test
    fun fakeClock_advancesDeterministically() {
        assertEquals(baselineInstant, fakeClock.instant())
        assertEquals(1, fakeClock.accessCount)

        fakeClock.advanceBy(Duration.ofHours(3))
        assertEquals(Instant.parse("2026-06-01T15:00:00Z"), fakeClock.instant())
        assertEquals(2, fakeClock.accessCount)

        fakeClock.advanceBy(60_000L) // 1 minute
        assertEquals(Instant.parse("2026-06-01T15:01:00Z"), fakeClock.instant())
    }

    @Test
    fun fakeClock_asAppClock_emitsAccurateTimeAndDate() {
        val appClock = fakeClock.asAppClock()
        val initialDateTime = LocalDateTime.of(2026, 6, 1, 12, 0, 0)

        assertEquals(initialDateTime, appClock.now())
        assertEquals(LocalDate.of(2026, 6, 1), appClock.todayDate())
        assertTrue(fakeClock.wasAccessed)
    }

    @Test
    fun clockInjectionCheck_assertClockQueried_succeedsWhenClockUsed() {
        val clock = FakeClock()
        clock.instant() // simulate injected clock query
        ClockInjectionCheck.assertClockQueried(clock)
    }

    @Test
    fun clockInjectionCheck_assertClockQueried_failsWhenClockBypassed() {
        val unqueriedClock = FakeClock()
        try {
            ClockInjectionCheck.assertClockQueried(unqueriedClock)
            fail("Expected AssertionError because clock was not queried")
        } catch (expected: AssertionError) {
            assertTrue(expected.message?.contains("Injected FakeClock was never queried") == true)
        }
    }

    @Test
    fun clockInjectionCheck_ruleFailsOnFinished_whenClockWasNotQueried() {
        val unqueriedClock = FakeClock()
        val check = ClockInjectionCheck(unqueriedClock, requireAccess = true)
        val description = Description.createTestDescription(ClockInjectionCheckTest::class.java, "testMethod")

        check.starting(description)
        assertFalse(unqueriedClock.wasAccessed)

        try {
            check.finished(description)
            fail("Expected rule finished() to throw AssertionError")
        } catch (expected: AssertionError) {
            assertTrue(expected.message?.contains("did not query the injected FakeClock") == true)
        }
    }

    @Test
    fun clockInjectionCheck_ruleSucceedsOnFinished_whenClockWasQueried() {
        val clock = FakeClock()
        val check = ClockInjectionCheck(clock, requireAccess = true)
        val description = Description.createTestDescription(ClockInjectionCheckTest::class.java, "testMethod")

        check.starting(description)
        clock.millis() // accessed!
        check.finished(description) // Should not throw
    }
}
