package com.pixelquest.app.domain.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Aggregate numbers sent to Gemini for the daily reminder message pack. No task names,
 * descriptions or other free text: the same privacy rule as AI Coach insights.
 */
data class EncouragementPackInput(
    val currentStreak: Int,
    val completionRatePercent7d: Int,
    val activeTaskCount: Int,
    val tone: HabitInsightTone
)

object EncouragementPackPrompt {

    const val MESSAGE_COUNT = 5

    fun systemInstruction(tone: HabitInsightTone): String {
        val voice = when (tone) {
            HabitInsightTone.GAMIFIED_HEROIC ->
                "a cheerful retro RPG guide. Light quest and hero language is welcome; no XP numbers or promises of rewards."
            HabitInsightTone.SIMPLE_MINIMALIST ->
                "a calm, practical habit coach. Plain language only: no games, quests, heroes, streaks, points or levels."
        }
        return """
            You write short encouraging lines that appear inside a habit app's task reminders. Speak as $voice
            Rules: each line is one sentence under ${EncouragementSanitizer.MAX_CHARS} characters, kind and specific to the
            numbers given, never guilt-tripping, no emoji, no hashtags, no links, no quotation marks, and never invent task names.
            Respond only with JSON: {"messages": ["...", "..."]} containing exactly $MESSAGE_COUNT lines.
        """.trimIndent()
    }

    fun prompt(input: EncouragementPackInput): String = """
        Current streak (days): ${input.currentStreak.coerceAtLeast(0)}
        Task completion rate over the last 7 days: ${input.completionRatePercent7d.coerceIn(0, 100)}%
        Active habits: ${input.activeTaskCount.coerceAtLeast(0)}
        Write $MESSAGE_COUNT reminder lines.
    """.trimIndent()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Returns the raw "messages" strings, or an empty list if the response isn't the expected JSON. */
    fun parse(raw: String): List<String> {
        val body = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return try {
            json.parseToJsonElement(body).jsonObject["messages"]
                ?.jsonArray
                ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
