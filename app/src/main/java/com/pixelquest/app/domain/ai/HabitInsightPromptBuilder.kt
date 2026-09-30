package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity

/**
 * Aggregated habit metrics safe for AI analysis (strictly zero PII).
 */
data class HabitTelemetrySummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val perfectDaysCount: Int,
    val playerLevel: Int,
    val categoryRatios: Map<String, CategoryMetric> = emptyMap(),
    val recent7DaysCompletionRate: Float = 0f,
    val recent7DaysMissedCount: Int = 0
)

data class CategoryMetric(
    val scheduledCount: Int,
    val completedCount: Int
) {
    val completionPercentage: Int
        get() = if (scheduledCount > 0) ((completedCount.toFloat() / scheduledCount) * 100).toInt() else 0
}

/**
 * Constructs privacy-safe, anonymized prompts and system directives for Google Gemini
 * habit analysis from local Room data.
 */
object HabitInsightPromptBuilder {

    /**
     * Builds aggregated telemetry from local Room entities, ensuring no raw personal data
     * (such as username, email, or verbatim task names) is retained.
     */
    fun buildTelemetrySummary(
        streak: StreakEntity?,
        profile: UserProfileEntity?,
        tasks: List<TaskEntity>,
        logs: List<TaskCompletionLogEntity>
    ): HabitTelemetrySummary {
        val currentStreak = streak?.currentStreak ?: 0
        val longestStreak = streak?.longestStreak ?: 0
        val perfectDays = streak?.perfectDaysCount ?: 0
        val level = profile?.level ?: 1

        val taskCategoryMap = tasks.associate { it.id to it.category.name }

        val categoryScheduled = mutableMapOf<String, Int>()
        val categoryCompleted = mutableMapOf<String, Int>()

        logs.forEach { log ->
            val catName = taskCategoryMap[log.taskId] ?: "GENERAL"
            categoryScheduled[catName] = (categoryScheduled[catName] ?: 0) + 1
            if (log.wasCompleted) {
                categoryCompleted[catName] = (categoryCompleted[catName] ?: 0) + 1
            }
        }

        val categoryRatios = categoryScheduled.mapValues { (cat, sched) ->
            CategoryMetric(
                scheduledCount = sched,
                completedCount = categoryCompleted[cat] ?: 0
            )
        }

        val totalLogs = logs.size
        val completedLogs = logs.count { it.wasCompleted }
        val completionRate = if (totalLogs > 0) completedLogs.toFloat() / totalLogs else 0f
        val missedCount = totalLogs - completedLogs

        return HabitTelemetrySummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            perfectDaysCount = perfectDays,
            playerLevel = level,
            categoryRatios = categoryRatios,
            recent7DaysCompletionRate = completionRate,
            recent7DaysMissedCount = missedCount
        )
    }

    /**
     * Constructs the user prompt containing anonymized habit metrics.
     */
    fun buildPrompt(
        telemetry: HabitTelemetrySummary,
        tone: HabitInsightTone = HabitInsightTone.GAMIFIED_HEROIC
    ): String {
        val sb = StringBuilder()
        sb.appendLine("HABIT TELEMETRY SUMMARY:")
        sb.appendLine("- Streak Momentum: Current Streak = ${telemetry.currentStreak} days | Best = ${telemetry.longestStreak} days | Perfect Days = ${telemetry.perfectDaysCount}")
        sb.appendLine("- Player Progression Tier: Level ${telemetry.playerLevel}")

        if (telemetry.recent7DaysCompletionRate > 0 || telemetry.recent7DaysMissedCount > 0) {
            val pct = (telemetry.recent7DaysCompletionRate * 100).toInt()
            sb.appendLine("- Recent Consistency: $pct% completion rate (${telemetry.recent7DaysMissedCount} missed entries)")
        } else {
            sb.appendLine("- Recent Consistency: New journey, baseline history building.")
        }

        sb.appendLine("- Performance by Category:")
        if (telemetry.categoryRatios.isEmpty()) {
            sb.appendLine("  * No category history logged yet.")
        } else {
            telemetry.categoryRatios.forEach { (cat, metric) ->
                sb.appendLine("  * $cat: ${metric.completionPercentage}% (${metric.completedCount}/${metric.scheduledCount} completed)")
            }
        }

        sb.appendLine()
        sb.appendLine("TASK:")
        if (tone == HabitInsightTone.SIMPLE_MINIMALIST) {
            sb.appendLine("Provide a thoughtful, calm habit coaching insight based on these metrics. Encourage consistency and offer 1 practical adjustment.")
        } else {
            sb.appendLine("Provide an epic 8-bit questmaster debrief based on these metrics. Celebrate heroic consistency and issue 1 tactical quest suggestion.")
        }
        sb.appendLine("Respond strictly in valid JSON matching schema: {\"summary\": \"...\", \"suggestion\": \"...\", \"encouragement\": \"...\", \"highlightCategory\": \"...\", \"specificTaskCallout\": \"...\"}")
        return sb.toString()
    }

    /**
     * Constructs the system instruction directing model persona, tone, and formatting constraints.
     */
    fun buildSystemInstruction(
        tone: HabitInsightTone = HabitInsightTone.GAMIFIED_HEROIC
    ): String {
        return if (tone == HabitInsightTone.SIMPLE_MINIMALIST) {
            """
            You are an expert, empathetic habit coach.
            Analyze the user's habit telemetry and provide clear, objective, and supportive observations.
            Provide actionable suggestions to maintain or build consistency.
            Do NOT use gaming metaphors, quests, battle references, XP, levels, or arcade terminology.
            Be calm, encouraging, and concise.
            You MUST output your response strictly as valid JSON conforming to the requested schema.
            """.trimIndent()
        } else {
            """
            You are the Questmaster of PixelQuest, a retro 8-bit RPG habit tracker.
            Analyze the adventurer's recent habit telemetry and provide an empowering, insightful quest debrief.
            Frame consistency as battle resilience, streaks as hero momentum, and categories as quest disciplines.
            Be encouraging, authentic, and concise. Avoid condescension.
            You MUST output your response strictly as valid JSON conforming to the requested schema.
            """.trimIndent()
        }
    }
}
