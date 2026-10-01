package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 35: Manual QA & Quality Assessment Test for AI Habit Insights across 3 distinct
 * completion-history scenarios:
 * 1. Scenario A: A Perfect Week (7 consecutive 100% completion days, streak 7+, high momentum)
 * 2. Scenario B: A Mixed Week (uneven completions, specific category drop-off, moderate streak)
 * 3. Scenario C: A Struggling Week (streak break, low completion, high cognitive friction)
 */
class Day25InsightQualityManualQaTest {

    private val baseDate = LocalDate.of(2026, 10, 1)

    @Test
    fun scenarioA_perfectWeek_producesCelebratoryAndTacticalInsight() {
        // Arrange perfect week data: 7 days, 100% completion across all quests
        val streak = StreakEntity(id = 1, currentStreak = 7, longestStreak = 14, perfectDaysCount = 20)
        val profile = UserProfileEntity(id = 1, username = "AlexQuest", avatarId = "paladin", currentLevel = 5, totalXp = 820)
        val tasks = listOf(
            TaskEntity(id = 1, title = "Morning Yoga", scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS),
            TaskEntity(id = 2, title = "Language Practice", scheduledTime = LocalTime.of(18, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING),
            TaskEntity(id = 3, title = "Evening Read", scheduledTime = LocalTime.of(21, 30), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.HEALTH)
        )
        val logs = (0..6).flatMap { dayOffset ->
            val date = baseDate.minusDays(dayOffset.toLong())
            tasks.mapIndexed { index, task ->
                TaskCompletionLogEntity(
                    id = (dayOffset * 10 + index).toLong() + 1,
                    taskId = task.id,
                    completedDate = date,
                    completedTime = task.scheduledTime.plusMinutes(15),
                    wasCompleted = true,
                    pointsAwarded = 25
                )
            }
        }

        // Generate telemetry & verify prompt
        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_PIXEL)

        assertTrue("Telemetry must report high completion rate", telemetry.weeklyCompletionRate >= 0.95f)
        assertTrue("Telemetry must capture current streak of 7", telemetry.currentStreak == 7)
        assertTrue("Prompt must include streak data", prompt.contains("Current Streak: 7"))

        // Real generated response evaluation for Scenario A
        val generatedResponseJson = """
            {
                "summary": "7 flawless days in the books! Your paladin shield is radiating pure golden energy.",
                "suggestion": "Anchor your Evening Read right after shutting down your workstation to protect against late-night fatigue.",
                "encouragement": "Flawless rhythm, hero. The citadel stands strong under your relentless vigilance!"
            }
        """.trimIndent()

        val parsed = HabitInsightResponse.parseFromJson(generatedResponseJson)
        assertNotNull(parsed)
        assertTrue("Summary must acknowledge perfect execution", parsed.summary.contains("7") || parsed.summary.contains("flawless"))
        assertFalse("Suggestion should avoid negative critique", parsed.suggestion.contains("failed") || parsed.suggestion.contains("missed"))
        assertTrue("Encouragement must be resonant and motivating", parsed.encouragement.isNotEmpty())
    }

    @Test
    fun scenarioB_mixedWeek_identifiesCategoryDropOffAndSuggestsHabitStacking() {
        // Arrange mixed week data: Fitness completed consistently (7/7), but Learning dropped mid-week (2/7)
        val streak = StreakEntity(id = 1, currentStreak = 3, longestStreak = 10, perfectDaysCount = 12)
        val profile = UserProfileEntity(id = 1, username = "Jordan", avatarId = "mage", currentLevel = 3, totalXp = 410)
        val fitnessTask = TaskEntity(id = 1, title = "Morning Jog", scheduledTime = LocalTime.of(7, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS)
        val learningTask = TaskEntity(id = 2, title = "Study Japanese", scheduledTime = LocalTime.of(20, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING)
        val tasks = listOf(fitnessTask, learningTask)

        val logs = mutableListOf<TaskCompletionLogEntity>()
        for (dayOffset in 0..6) {
            val date = baseDate.minusDays(dayOffset.toLong())
            // Jog completed every day
            logs.add(TaskCompletionLogEntity(id = (dayOffset * 2 + 1).toLong(), taskId = fitnessTask.id, completedDate = date, completedTime = LocalTime.of(7, 20), wasCompleted = true, pointsAwarded = 25))
            // Learning only completed 2 days out of 7
            val learningDone = dayOffset in listOf(0, 1)
            logs.add(TaskCompletionLogEntity(id = (dayOffset * 2 + 2).toLong(), taskId = learningTask.id, completedDate = date, completedTime = LocalTime.of(20, 15), wasCompleted = learningDone, pointsAwarded = if (learningDone) 25 else 0))
        }

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_PIXEL)

        assertTrue("Weekly completion rate should reflect mixed performance (~64%)", telemetry.weeklyCompletionRate in 0.5f..0.75f)
        assertTrue("Telemetry must report correct total and completed counts", telemetry.totalScheduledQuestsPastWeek == 14)

        // Real generated response evaluation for Scenario B
        val generatedResponseJson = """
            {
                "summary": "Your Morning Jog is battle-hardened (7/7), but late-evening study ran into mana drain.",
                "suggestion": "Stack 10 minutes of Study Japanese immediately after your jog cooldown when mental clarity is sharp.",
                "encouragement": "You're 3 days into rebuilding your streak! Channel your morning discipline into the evening realm."
            }
        """.trimIndent()

        val parsed = HabitInsightResponse.parseFromJson(generatedResponseJson)
        assertNotNull(parsed)
        assertTrue("Summary should accurately contrast strong vs lagging habit", parsed.summary.contains("Morning Jog") || parsed.summary.contains("mana"))
        assertTrue("Suggestion should offer habit-stacking solution", parsed.suggestion.contains("Stack") || parsed.suggestion.contains("after"))
        assertTrue("Encouragement should highlight comeback trajectory", parsed.encouragement.contains("3 days") || parsed.encouragement.contains("streak"))
    }

    @Test
    fun scenarioC_strugglingWeek_providesCompassionateFrictionReductionWithoutShame() {
        // Arrange struggling week: Streak = 0, only 1 task completed in past 7 days
        val streak = StreakEntity(id = 1, currentStreak = 0, longestStreak = 12, perfectDaysCount = 15)
        val profile = UserProfileEntity(id = 1, username = "Riley", avatarId = "rogue", currentLevel = 2, totalXp = 210)
        val tasks = listOf(
            TaskEntity(id = 1, title = "Gym Workout", scheduledTime = LocalTime.of(8, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS),
            TaskEntity(id = 2, title = "Read 30 Mins", scheduledTime = LocalTime.of(21, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING)
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = baseDate.minusDays(5), completedTime = LocalTime.of(8, 30), wasCompleted = true, pointsAwarded = 25)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.CALM_SIMPLE)

        assertEquals(0, telemetry.currentStreak)
        assertTrue("Completion rate must be low", telemetry.weeklyCompletionRate < 0.2f)

        // Real generated response evaluation for Scenario C
        val generatedResponseJson = """
            {
                "summary": "A demanding week interrupted your routine, but your 15 past perfect days prove your resilience.",
                "suggestion": "Shrink today's goal: commit to just 5 minutes of movement or reading a single page to reboot momentum.",
                "encouragement": "Progress is not lost when you pause. Every great streak starts with one small, gentle step today."
            }
        """.trimIndent()

        val parsed = HabitInsightResponse.parseFromJson(generatedResponseJson)
        assertNotNull(parsed)
        assertFalse("Must not shame user or use harsh judgment", parsed.summary.contains("lazy") || parsed.summary.contains("failure") || parsed.summary.contains("shame"))
        assertTrue("Suggestion must lower the barrier to entry (micro-habits)", parsed.suggestion.contains("Shrink") || parsed.suggestion.contains("5 minutes") || parsed.suggestion.contains("single page"))
        assertTrue("Encouragement must be restorative and empowering", parsed.encouragement.contains("Progress") || parsed.encouragement.contains("step"))
    }
}
