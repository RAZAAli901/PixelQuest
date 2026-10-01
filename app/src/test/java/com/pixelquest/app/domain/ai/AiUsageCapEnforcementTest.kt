package com.pixelquest.app.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Step 32: Unit tests for hard daily and monthly Gemini API cap-enforcement logic
 * across boundary conditions and date rollovers.
 */
class AiUsageCapEnforcementTest {

    private lateinit var usageTracker: InMemoryAiUsageTracker

    @Before
    fun setUp() {
        usageTracker = InMemoryAiUsageTracker(
            maxDaily = AiUsagePolicy.MAX_CALLS_PER_DAY,     // 4 calls / day
            maxMonthly = AiUsagePolicy.MAX_CALLS_PER_MONTH   // 60 calls / month
        )
    }

    @Test
    fun initialState_allowsCallsWithFullQuota() {
        val today = LocalDate.of(2026, 10, 1)

        assertTrue("Initial state must permit API calls", usageTracker.canMakeCall(today))
        assertEquals(4, usageTracker.getDailyCallsRemaining(today))
        assertEquals(60, usageTracker.getMonthlyCallsRemaining(today))
        assertFalse(usageTracker.isDailyCapReached(today))
        assertFalse(usageTracker.isMonthlyCapReached(today))
    }

    @Test
    fun dailyCap_permitsExactlyFourCalls_andBlocksFifthCall() {
        val today = LocalDate.of(2026, 10, 1)

        // Make 4 allowed calls
        for (i in 1..4) {
            assertTrue("Call $i must be permitted under daily cap", usageTracker.canMakeCall(today))
            usageTracker.recordCall(today)
            assertEquals(4 - i, usageTracker.getDailyCallsRemaining(today))
        }

        // 4th call reached cap
        assertTrue("Daily cap must be reached after 4 calls", usageTracker.isDailyCapReached(today))
        assertEquals(0, usageTracker.getDailyCallsRemaining(today))
        assertFalse("5th call must be strictly blocked by daily cap", usageTracker.canMakeCall(today))

        // Monthly count should be 4
        assertEquals(4, usageTracker.getDailyCallsCount(today))
        assertEquals(4, usageTracker.getMonthlyCallsCount(today))
        assertEquals(56, usageTracker.getMonthlyCallsRemaining(today))
    }

    @Test
    fun dateRollover_resetsDailyQuota_whilePreservingMonthlyTotal() {
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)

        // Consume day 1 limit
        repeat(4) { usageTracker.recordCall(day1) }
        assertFalse("Day 1 must be blocked after 4 calls", usageTracker.canMakeCall(day1))

        // Next calendar day arrives
        assertTrue("Day 2 must allow new calls after midnight rollover", usageTracker.canMakeCall(day2))
        assertEquals(4, usageTracker.getDailyCallsRemaining(day2))
        assertFalse(usageTracker.isDailyCapReached(day2))

        // Monthly tally must preserve day 1 calls
        assertEquals(56, usageTracker.getMonthlyCallsRemaining(day2))
        assertEquals(4, usageTracker.getMonthlyCallsCount(day2))
    }

    @Test
    fun monthlyCap_blocksCallsOnceSixtyLimitIsHit_evenIfDailyCapNotExceeded() {
        var currentDate = LocalDate.of(2026, 10, 1)

        // Simulate 15 days of 4 calls each = 60 calls
        for (day in 1..15) {
            currentDate = LocalDate.of(2026, 10, day)
            repeat(4) {
                assertTrue("Calls within month limit should pass", usageTracker.canMakeCall(currentDate))
                usageTracker.recordCall(currentDate)
            }
        }

        // Total calls = 60
        assertEquals(60, usageTracker.getMonthlyCallsCount(currentDate))
        assertEquals(0, usageTracker.getMonthlyCallsRemaining(currentDate))
        assertTrue("Monthly cap must be triggered", usageTracker.isMonthlyCapReached(currentDate))

        // Day 16 arrives: daily count would be 0, but monthly cap is exhausted
        val day16 = LocalDate.of(2026, 10, 16)
        assertTrue("Daily count would be fresh", usageTracker.getDailyCallsRemaining(day16) > 0)
        assertFalse("Call must still be blocked because monthly cap is reached", usageTracker.canMakeCall(day16))
    }

    @Test
    fun monthRollover_resetsBothMonthlyAndDailyCounters() {
        val endOfOctober = LocalDate.of(2026, 10, 31)
        // Hit monthly cap
        repeat(60) { usageTracker.recordCall(endOfOctober) }
        assertFalse(usageTracker.canMakeCall(endOfOctober))

        // 1st of November
        val startOfNovember = LocalDate.of(2026, 11, 1)
        assertTrue("New month must reset quota and permit calls", usageTracker.canMakeCall(startOfNovember))
        assertEquals(4, usageTracker.getDailyCallsRemaining(startOfNovember))
        assertEquals(60, usageTracker.getMonthlyCallsRemaining(startOfNovember))
        assertEquals(0, usageTracker.getDailyCallsCount(startOfNovember))
        assertEquals(0, usageTracker.getMonthlyCallsCount(startOfNovember))
    }

    @Test
    fun policyMessageFormatting_producesGracefulExplanations() {
        val dailyMsg = AiUsagePolicy.formatDailyLimitMessage()
        assertTrue("Daily message must mention 4/day limit", dailyMsg.contains("4/day"))
        assertTrue("Daily message must advise checking back tomorrow", dailyMsg.contains("tomorrow", ignoreCase = true))

        val monthlyMsg = AiUsagePolicy.formatMonthlyLimitMessage()
        assertTrue("Monthly message must mention 60/month limit", monthlyMsg.contains("60/month"))
        assertTrue("Monthly message must advise checking back next month", monthlyMsg.contains("next month", ignoreCase = true))
    }
}
