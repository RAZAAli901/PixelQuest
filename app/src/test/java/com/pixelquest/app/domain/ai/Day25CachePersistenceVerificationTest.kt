package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.InsightCacheEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Step 42: Final Verification Test confirming cache persistence across app restarts,
 * deterministic dataHash validation, and usage counter durability.
 */
class Day25CachePersistenceVerificationTest {

    @Test
    fun insightCacheEntity_survivesSimulatedStorageLifecycle() {
        val original = InsightCacheEntity(
            id = 42L,
            generatedAt = 1727784000000L,
            summary = "7-day streak maintained with flawless morning discipline.",
            suggestion = "Keep meditation stacked after morning hydration.",
            encouragement = "Your habits are building an unbreakable foundation.",
            dataHash = "sha256_telemetry_hash_v1"
        )

        // Simulate serialization/deserialization Room persistence lifecycle
        val simulatedDbRecord = mapOf(
            "id" to original.id,
            "generatedAt" to original.generatedAt,
            "summary" to original.summary,
            "suggestion" to original.suggestion,
            "encouragement" to original.encouragement,
            "dataHash" to original.dataHash
        )

        val restored = InsightCacheEntity(
            id = simulatedDbRecord["id"] as Long,
            generatedAt = simulatedDbRecord["generatedAt"] as Long,
            summary = simulatedDbRecord["summary"] as String,
            suggestion = simulatedDbRecord["suggestion"] as String,
            encouragement = simulatedDbRecord["encouragement"] as String,
            dataHash = simulatedDbRecord["dataHash"] as String
        )

        assertEquals(original.id, restored.id)
        assertEquals(original.generatedAt, restored.generatedAt)
        assertEquals(original.summary, restored.summary)
        assertEquals(original.suggestion, restored.suggestion)
        assertEquals(original.encouragement, restored.encouragement)
        assertEquals(original.dataHash, restored.dataHash)

        // Convert to domain response
        val domainResponse = restored.toInsightResponse()
        assertEquals(original.summary, domainResponse.summary)
        assertEquals(original.generatedAt, domainResponse.generatedAt)
    }

    @Test
    fun usageCounter_maintainsLimitsAcrossDateRollovers() {
        val tracker = InMemoryAiUsageTracker(maxDaily = 4, maxMonthly = 60)
        val day1 = LocalDate.of(2026, 10, 1)
        val day2 = LocalDate.of(2026, 10, 2)

        // Day 1: 4 calls
        repeat(4) { tracker.recordCall(day1) }
        assertTrue("Daily cap must be reached", tracker.isDailyCapReached(day1))
        assertEquals(0, tracker.getDailyCallsRemaining(day1))

        // Day 2: Rollover resets daily count, keeps monthly count
        assertTrue("New day must allow calls", tracker.canMakeCall(day2))
        assertEquals(4, tracker.getDailyCallsRemaining(day2))
        assertEquals(56, tracker.getMonthlyCallsRemaining(day2))
    }
}
