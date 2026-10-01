package com.pixelquest.app.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Step 8: Unit tests verifying user-facing cooldown and rate limit messaging formatting.
 */
class AiRateLimitFormatterTest {

    @Test
    fun formatCooldownMessage_multipleHours() {
        // 6 hours (21600 seconds)
        val msg6h = AiRateLimitFormatter.formatCooldownMessage(6 * 3600L)
        assertEquals("Check back in 6 hours for a fresh insight.", msg6h)

        // 3 hours 30 mins (12600 seconds) -> rounded up to 4 hours
        val msg3h30 = AiRateLimitFormatter.formatCooldownMessage(12600L)
        assertEquals("Check back in 4 hours for a fresh insight.", msg3h30)

        // 2 hours 1 second -> rounded up to 3 hours
        val msg2h = AiRateLimitFormatter.formatCooldownMessage(7201L)
        assertEquals("Check back in 3 hours for a fresh insight.", msg2h)
    }

    @Test
    fun formatCooldownMessage_singleHour() {
        // 1 hour exactly
        val msg1h = AiRateLimitFormatter.formatCooldownMessage(3600L)
        assertEquals("Check back in 1 hour for a fresh insight.", msg1h)

        // 1 hour 15 mins (4500 seconds)
        val msg1h15 = AiRateLimitFormatter.formatCooldownMessage(4500L)
        assertEquals("Check back in 1 hour for a fresh insight.", msg1h15)
    }

    @Test
    fun formatCooldownMessage_minutes() {
        // 45 minutes (2700 seconds)
        val msg45m = AiRateLimitFormatter.formatCooldownMessage(45 * 60L)
        assertEquals("Check back in 45 minutes for a fresh insight.", msg45m)

        // 1 minute (60 seconds)
        val msg1m = AiRateLimitFormatter.formatCooldownMessage(60L)
        assertEquals("Check back in 1 minutes for a fresh insight.", msg1m)
    }

    @Test
    fun formatCooldownMessage_seconds() {
        // 45 seconds
        val msg45s = AiRateLimitFormatter.formatCooldownMessage(45L)
        assertEquals("Check back in 45 seconds for a fresh insight.", msg45s)

        // 1 second
        val msg1s = AiRateLimitFormatter.formatCooldownMessage(1L)
        assertEquals("Check back in 1 seconds for a fresh insight.", msg1s)
    }

    @Test
    fun formatCooldownMessage_zeroOrNegative() {
        val msgZero = AiRateLimitFormatter.formatCooldownMessage(0L)
        assertEquals("You can generate a fresh insight now.", msgZero)

        val msgNeg = AiRateLimitFormatter.formatCooldownMessage(-10L)
        assertEquals("You can generate a fresh insight now.", msgNeg)
    }
}
