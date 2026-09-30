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
    val recent7DaysMissedCount: Int = 0,
    val excludedPiiTerms: Set<String> = emptySet()
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
 *
 * Implements active PII sanitization to guarantee no personal information (emails, usernames,
 * phone numbers, or verbatim task names) ever enters the prompt pipeline.
 */
object HabitInsightPromptBuilder {

    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val PHONE_REGEX = Regex("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b")

    /**
     * Actively sanitizes a string by redacting emails, phone numbers, and any explicitly
     * forbidden terms (e.g. usernames, display names, raw task titles).
     */
    fun sanitizePromptText(
        input: String,
        forbiddenTerms: Set<String> = emptySet()
    ): String {
        var sanitized = input

        // 1. Actively strip emails
        sanitized = sanitized.replace(EMAIL_REGEX, "[REDACTED_EMAIL]")

        // 2. Actively strip phone numbers
        sanitized = sanitized.replace(PHONE_REGEX, "[REDACTED_PHONE]")

        // 3. Actively strip specific forbidden PII terms
        forbiddenTerms
            .filter { it.isNotBlank() && it.length > 1 }
            .forEach { term ->
                sanitized = sanitized.replace(term, "[REDACTED_PII]", ignoreCase = true)
            }

        return sanitized
    }

    /**
     * Builds aggregated telemetry from local Room entities, ensuring no raw personal data
     * (such as username, email, or verbatim task names) is retained.
     * Actively registers forbidden PII terms from the profile and tasks for defense-in-depth sanitization.
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

        // Collect all potential PII terms from entities for active exclusion
        val piiBlacklist = mutableSetOf<String>()
        profile?.username?.let { if (it.isNotBlank()) piiBlacklist.add(it) }
        profile?.leaderboardDisplayName?.let { if (it.isNotBlank()) piiBlacklist.add(it) }
        profile?.supabaseUserId?.let { if (it.isNotBlank()) piiBlacklist.add(it) }
        tasks.forEach { task ->
            if (task.name.isNotBlank()) piiBlacklist.add(task.name)
            if (task.description.isNotBlank()) piiBlacklist.add(task.description)
        }

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
            recent7DaysMissedCount = missedCount,
            excludedPiiTerms = piiBlacklist
        )
    }

    /**
     * Constructs the user prompt containing anonymized habit metrics,
     * applying active sanitization across the entire prompt body.
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
            sb.appendLine("Provide a thoughtful, calm habit coaching insight based on these metrics. Focus on steady routines, sustainable cadence, and practical adjustments. Omit all gaming, quest, battle, or fantasy references.")
        } else {
            sb.appendLine("Provide an epic 8-bit questmaster debrief based on these metrics. Celebrate heroic consistency and issue 1 tactical quest suggestion.")
        }
        sb.appendLine("Respond strictly in valid JSON matching schema: {\"summary\": \"...\", \"suggestion\": \"...\", \"encouragement\": \"...\", \"highlightCategory\": \"...\", \"specificTaskCallout\": \"...\"}")

        // Active sanitization pass
        return sanitizePromptText(sb.toString(), telemetry.excludedPiiTerms)
    }

    /**
     * Overload constructing prompt from HabitInsightToneHook and Simple Mode state.
     */
    fun buildPrompt(
        telemetry: HabitTelemetrySummary,
        toneHook: HabitInsightToneHook,
        isSimpleModeEnabled: Boolean
    ): String {
        val tone = toneHook.resolveTone(isSimpleModeEnabled)
        return buildPrompt(telemetry, tone)
    }

    /**
     * Constructs the system instruction using HabitInsightToneHook for tone adaptation.
     */
    fun buildSystemInstruction(
        toneHook: HabitInsightToneHook,
        isSimpleModeEnabled: Boolean
    ): String {
        val tone = toneHook.resolveTone(isSimpleModeEnabled)
        val guidance = toneHook.getSystemPromptGuidance(tone)
        return buildSystemInstructionWithGuidance(tone, guidance)
    }

    /**
     * Constructs the system instruction directing model persona, tone, and formatting constraints.
     */
    fun buildSystemInstruction(
        tone: HabitInsightTone = HabitInsightTone.GAMIFIED_HEROIC
    ): String {
        val defaultHook = DefaultHabitInsightToneHook()
        val guidance = defaultHook.getSystemPromptGuidance(tone)
        return buildSystemInstructionWithGuidance(tone, guidance)
    }

    private fun buildSystemInstructionWithGuidance(
        tone: HabitInsightTone,
        guidance: String
    ): String {
        return if (tone == HabitInsightTone.SIMPLE_MINIMALIST) {
            """
            You are an expert, empathetic habit coach.
            Tone Guidance: $guidance
            Analyze the user's habit telemetry and provide clear, objective, and supportive observations.
            Provide actionable suggestions to maintain or build consistency.
            Do NOT use gaming metaphors, quests, battle references, XP, levels, or arcade terminology.
            Be calm, encouraging, and concise.
            You MUST output your response strictly as valid JSON conforming to the requested schema.
            """.trimIndent()
        } else {
            """
            You are the Questmaster of PixelQuest, a retro 8-bit RPG habit tracker.
            Tone Guidance: $guidance
            Analyze the adventurer's recent habit telemetry and provide an empowering, insightful quest debrief.
            Frame consistency as battle resilience, streaks as hero momentum, and categories as quest disciplines.
            Be encouraging, authentic, and concise. Avoid condescension.
            You MUST output your response strictly as valid JSON conforming to the requested schema.
            """.trimIndent()
        }
    }
}
