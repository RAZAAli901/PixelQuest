package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.repository.HabitInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 35: Unit test verifying that the rate-limit groundwork value (minimum 6-hour interval
 * defined in Section C Step 16) is strictly enforced by the debug trigger.
 */
class AiInsightThrottleTest {

    private class CountingHabitInsightRepository : HabitInsightRepository {
        var callCount = 0
        var resultToReturn: GeminiResult<HabitInsightResponse> = GeminiResult.Success(
            HabitInsightResponse(
                summary = "Thriving habits!",
                suggestion = "Keep going.",
                encouragement = "Great job!",
                highlightCategory = "FITNESS"
            )
        )

        override suspend fun generateHabitInsight(forceRefresh: Boolean): GeminiResult<HabitInsightResponse> {
            callCount++
            return resultToReturn
        }

        override val latestInsight: Flow<HabitInsightResponse?> = flowOf(null)
    }

    @Test
    fun debugTrigger_enforces6HourMinimumInterval() = runBlocking {
        var simulatedTimeMs = 1_000_000L
        val mockRepo = CountingHabitInsightRepository()

        val trigger = DebugAiInsightTrigger(
            habitInsightRepository = mockRepo,
            clock = { simulatedTimeMs }
        )

        // 1. First trigger at T = 1,000,000 ms -> should succeed
        val firstResult = trigger.triggerDirectly()
        assertTrue("First call must succeed", firstResult is GeminiResult.Success)
        assertEquals("Repository must be invoked once", 1, mockRepo.callCount)
        assertEquals("SUCCESS", trigger.debugStatus.value)

        // 2. Second trigger at T + 1 hour (3,600,000 ms later) -> should be throttled
        simulatedTimeMs += 3_600_000L
        val throttledResult = trigger.triggerDirectly()
        assertTrue("Second call within 6 hours must be RateLimited", throttledResult is GeminiResult.RateLimited)
        val rateLimited = throttledResult as GeminiResult.RateLimited
        val expectedRemainingSeconds = (DebugAiInsightTrigger.MIN_CALL_INTERVAL_MS - 3_600_000L) / 1000
        assertEquals(expectedRemainingSeconds, rateLimited.retryAfterSeconds)
        assertTrue("Status must reflect throttling", trigger.debugStatus.value.startsWith("THROTTLED"))
        assertEquals("Repository must NOT be invoked a second time", 1, mockRepo.callCount)

        // 3. Third trigger at T + 5 hours 59 minutes (still slightly under 6 hours) -> still throttled
        simulatedTimeMs = 1_000_000L + DebugAiInsightTrigger.MIN_CALL_INTERVAL_MS - 1_000L
        val stillThrottledResult = trigger.triggerDirectly()
        assertTrue("Call just before 6 hours must still be throttled", stillThrottledResult is GeminiResult.RateLimited)
        assertEquals("Repository still not called", 1, mockRepo.callCount)

        // 4. Fourth trigger at T + 6 hours + 10 milliseconds -> allowed!
        simulatedTimeMs = 1_000_000L + DebugAiInsightTrigger.MIN_CALL_INTERVAL_MS + 10L
        val allowedResult = trigger.triggerDirectly()
        assertTrue("Call after 6 hours must succeed", allowedResult is GeminiResult.Success)
        assertEquals("Repository must now be invoked a second time", 2, mockRepo.callCount)
        assertEquals("SUCCESS", trigger.debugStatus.value)
    }

    @Test
    fun debugTrigger_failedCallDoesNotLockOutRetries() = runBlocking {
        var simulatedTimeMs = 1_000_000L
        val mockRepo = CountingHabitInsightRepository()
        mockRepo.resultToReturn = GeminiResult.NetworkError(RuntimeException("Offline"))

        val trigger = DebugAiInsightTrigger(
            habitInsightRepository = mockRepo,
            clock = { simulatedTimeMs }
        )

        // First call fails with NetworkError
        val firstResult = trigger.triggerDirectly()
        assertTrue("First call returns NetworkError", firstResult is GeminiResult.NetworkError)
        assertEquals(1, mockRepo.callCount)

        // Second call 1 minute later should NOT be throttled because previous call was not Success
        simulatedTimeMs += 60_000L
        mockRepo.resultToReturn = GeminiResult.Success(
            HabitInsightResponse(
                summary = "Recovered!",
                suggestion = "Resume quest.",
                encouragement = "Never surrender.",
                highlightCategory = null
            )
        )

        val retryResult = trigger.triggerDirectly()
        assertTrue("Retry after failure must be allowed immediately", retryResult is GeminiResult.Success)
        assertEquals(2, mockRepo.callCount)
    }
}
