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
     */
    suspend fun generateHabitInsight(): GeminiResult<HabitInsightResponse>

    /**
     * Observable flow emitting the most recent generated insight, or null if none generated.
     */
    val latestInsight: Flow<HabitInsightResponse?>
}
