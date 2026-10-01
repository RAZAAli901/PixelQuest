package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pixelquest.app.domain.ai.HabitInsightResponse

/**
 * Step 1: Room database entity representing a locally cached AI habit insight.
 *
 * @param id Unique primary key.
 * @param summary High-level observation of recent habit patterns and momentum.
 * @param suggestion Actionable recommendation for building or sustaining consistency.
 * @param encouragement Motivating closing sentiment celebrating progress.
 * @param highlightCategory Standout category performance/focus area (non-PII).
 * @param specificTaskCallout Optional category-level focus tip (non-PII).
 * @param dataHash Cryptographic/deterministic hash of the underlying habit telemetry
 *                 (streaks, completion counts, level) enabling invalidation when data changes.
 * @param generatedAt Epoch timestamp (milliseconds) when the insight was synthesized.
 */
@Entity(tableName = "insight_cache")
data class InsightCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val summary: String,
    val suggestion: String,
    val encouragement: String,
    val highlightCategory: String? = null,
    val specificTaskCallout: String? = null,
    val dataHash: String,
    val generatedAt: Long = System.currentTimeMillis()
) {
    fun toInsightResponse(): HabitInsightResponse {
        return HabitInsightResponse(
            summary = summary,
            suggestion = suggestion,
            encouragement = encouragement,
            highlightCategory = highlightCategory,
            specificTaskCallout = specificTaskCallout,
            generatedAt = generatedAt
        )
    }

    companion object {
        fun fromInsightResponse(
            response: HabitInsightResponse,
            dataHash: String
        ): InsightCacheEntity {
            return InsightCacheEntity(
                summary = response.summary,
                suggestion = response.suggestion,
                encouragement = response.encouragement,
                highlightCategory = response.highlightCategory,
                specificTaskCallout = response.specificTaskCallout,
                dataHash = dataHash,
                generatedAt = response.generatedAt
            )
        }
    }
}
