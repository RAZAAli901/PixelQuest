package com.pixelquest.app.domain.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Defines the structured JSON output schema contract for Google Gemini habit insight responses.
 * Guarantees a parseable and deterministic output across model versions.
 */
object HabitInsightSchema {

    const val MIME_TYPE_JSON = "application/json"

    /**
     * Gemini-compliant responseSchema JSON structure for generationConfig.
     */
    val JSON_SCHEMA: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("summary") {
                put("type", "STRING")
                put("description", "2-3 sentences summarizing key habit patterns, consistency trends, and observations.")
            }
            putJsonObject("suggestion") {
                put("type", "STRING")
                put("description", "1 actionable, realistic tip to improve or maintain consistency.")
            }
            putJsonObject("encouragement") {
                put("type", "STRING")
                put("description", "1 inspiring, uplifting sentence celebrating progress and resilience.")
            }
            putJsonObject("highlightCategory") {
                put("type", "STRING")
                put("description", "The task category that stood out most (e.g. FITNESS, STUDY, PRODUCTIVITY), or null.")
            }
            putJsonObject("specificTaskCallout") {
                put("type", "STRING")
                put("description", "Optional category-level focus tip or advice. Strictly non-PII.")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("summary"))
            add(JsonPrimitive("suggestion"))
            add(JsonPrimitive("encouragement"))
        }
    }

    /**
     * Plaintext schema fallback directive embedded into prompt text for model robustness.
     */
    const val SCHEMA_PROMPT_DIRECTIVE: String = """
OUTPUT FORMAT REQUIREMENTS:
You MUST respond strictly with a valid JSON object matching this schema:
{
  "summary": "2-3 sentences summarizing habit patterns and observations",
  "suggestion": "1 actionable, realistic tip to improve consistency",
  "encouragement": "1 uplifting sentence celebrating resilience",
  "highlightCategory": "Category name or null",
  "specificTaskCallout": "Category focus advice or null"
}
"""
}
