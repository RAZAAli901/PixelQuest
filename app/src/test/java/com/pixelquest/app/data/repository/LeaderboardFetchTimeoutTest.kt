package com.pixelquest.app.data.repository

import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.remote.safeSupabaseCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardFetchTimeoutTest {

    @Test
    fun safeSupabaseCall_whenOperationExceedsTimeout_returnsNetworkError() = runBlocking {
        val shortTimeoutMs = 100L

        val result = safeSupabaseCall(timeoutMs = shortTimeoutMs) {
            // Simulate hanging network response
            delay(500L)
            "data"
        }

        assertTrue("Expected NetworkError on timeout", result is SupabaseResult.NetworkError)
        val error = result as SupabaseResult.NetworkError
        assertTrue("Error message should mention timeout", error.userMessage.contains("timed out", ignoreCase = true))
    }

    @Test
    fun safeSupabaseCall_whenOperationCompletesBeforeTimeout_returnsSuccess() = runBlocking {
        val timeoutMs = 1000L

        val result = safeSupabaseCall(timeoutMs = timeoutMs) {
            delay(50L)
            "quick-data"
        }

        assertTrue("Expected Success when completing within timeout", result is SupabaseResult.Success)
        val success = result as SupabaseResult.Success
        assertEquals("quick-data", success.data)
    }

    @Test
    fun leaderboardTimeoutConstant_isConfiguredToTenSeconds() {
        assertEquals(10_000L, LeaderboardRepositoryImpl.LEADERBOARD_TIMEOUT_MS)
    }
}
