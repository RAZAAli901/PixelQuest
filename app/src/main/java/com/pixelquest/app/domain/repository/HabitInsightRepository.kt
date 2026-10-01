package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.ai.HabitInsightResponse
import kotlinx.coroutines.flow.Flow

/**
 * Repository orchestrating AI habit insight generation:
 * gathers local Room telemetry, builds an anonymized prompt, calls GeminiClient,
 * parses structured JSON, and returns the typed result.
 */
interface HabitInsightRepository {

    /**
     * Executes the end-to-end habit insight pipeline.
     * When [forceRefresh] is true, ignores local cache and attempts a live refresh
     * (subject to rate limiting).
     */
    suspend fun generateHabitInsight(forceRefresh: Boolean = false): GeminiResult<HabitInsightResponse>

    /**
     * Observable flow emitting the most recent generated insight, or null if none generated.
     */
    val latestInsight: Flow<HabitInsightResponse?>

    /**
     * Calculates the remaining seconds before a live Gemini API call is permitted.
     * Returns 0 if no cooldown is active.
     */
    suspend fun getRemainingCooldownSeconds(): Long = 0L
}
