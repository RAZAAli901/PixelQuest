package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.domain.ai.HabitInsightResponse
import kotlinx.coroutines.flow.Flow

/**
 * Step 4: Repository interface orchestrating local caching of AI habit insights.
 */
interface InsightCacheRepository {

    /**
     * Persists a generated [HabitInsightResponse] along with the telemetry [dataHash].
     */
    suspend fun saveInsight(response: HabitInsightResponse, dataHash: String)

    /**
     * Retrieves the most recent cached insight, or null if no cache exists.
     */
    suspend fun getLatestInsight(): InsightCacheEntity?

    /**
     * Hot reactive stream observing changes to the latest cached insight.
     */
    fun observeLatestInsight(): Flow<InsightCacheEntity?>

    /**
     * Evaluates whether the currently cached insight is valid:
     * non-null, matching the given [currentDataHash], and newer than [maxAgeMillis].
     */
    suspend fun isCacheValid(currentDataHash: String, maxAgeMillis: Long): Boolean

    /**
     * Deletes cached insight entries older than [maxAgeMillis].
     */
    suspend fun clearExpired(maxAgeMillis: Long): Int

    /**
     * Purges all cached insights from local storage.
     */
    suspend fun clearAll(): Int
}
