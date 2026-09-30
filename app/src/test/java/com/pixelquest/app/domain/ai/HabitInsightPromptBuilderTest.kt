package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Step 12: Unit tests verifying HabitInsightPromptBuilder across four distinct user scenarios:
 * 1. New user with little data
 * 2. Established user with rich history
 * 3. All-perfect user
 * 4. Struggling user
 */
class HabitInsightPromptBuilderTest {

    private fun createTask(id: Long, category: TaskCategory): TaskEntity {
        return TaskEntity(
            id = id,
            name = "Task $id",
            description = "Description $id",
            scheduledDay = LocalDate.now(),
            scheduledTime = LocalTime.NOON,
            recurrenceType = RecurrenceType.DAILY,
            category = category
        )
    }

    @Test
    fun testNewUserWithLittleData() {
        val streak = StreakEntity(id = 1, currentStreak = 0, longestStreak = 0, perfectDaysCount = 0)
        val profile = UserProfileEntity(id = 1, username = "NewbieHero", avatarId = "warrior_1", level = 1)
        val tasks = listOf(createTask(1, TaskCategory.FITNESS))
        val logs = emptyList<TaskCompletionLogEntity>()

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_HEROIC)

        assertTrue(prompt.contains("Current Streak = 0"))
        assertTrue(prompt.contains("Best = 0"))
        assertTrue(prompt.contains("Level 1"))
        assertTrue(prompt.contains("New journey, baseline history building"))
        // Strict privacy check: personal username and verbatim task description must not be in prompt
        assertFalse(prompt.contains("NewbieHero"))
        assertFalse(prompt.contains("Description 1"))
    }

    @Test
    fun testEstablishedUserRichHistory() {
        val streak = StreakEntity(id = 1, currentStreak = 14, longestStreak = 28, perfectDaysCount = 42)
        val profile = UserProfileEntity(id = 1, username = "VeteranRanger", avatarId = "ranger_2", level = 8)
        val tasks = listOf(
            createTask(1, TaskCategory.FITNESS),
            createTask(2, TaskCategory.STUDY),
            createTask(3, TaskCategory.WORK)
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 2, taskId = 1, completedDate = LocalDate.now().minusDays(1), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 3, taskId = 2, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 4, taskId = 3, completedDate = LocalDate.now(), wasCompleted = false, pointsAwarded = 0)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_HEROIC)

        assertTrue(prompt.contains("Current Streak = 14"))
        assertTrue(prompt.contains("Best = 28"))
        assertTrue(prompt.contains("Perfect Days = 42"))
        assertTrue(prompt.contains("Level 8"))
        assertTrue(prompt.contains("FITNESS: 100%"))
        assertTrue(prompt.contains("STUDY: 100%"))
        assertTrue(prompt.contains("WORK: 0%"))
        assertFalse(prompt.contains("VeteranRanger"))
    }

    @Test
    fun testAllPerfectUser() {
        val streak = StreakEntity(id = 1, currentStreak = 30, longestStreak = 30, perfectDaysCount = 30)
        val profile = UserProfileEntity(id = 1, username = "FlawlessPaladin", avatarId = "paladin_3", level = 12)
        val tasks = listOf(
            createTask(1, TaskCategory.FITNESS),
            createTask(2, TaskCategory.HEALTH)
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 2, taskId = 2, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_HEROIC)

        assertTrue(prompt.contains("Current Streak = 30"))
        assertTrue(prompt.contains("Level 12"))
        assertTrue(prompt.contains("100% completion rate (0 missed entries)"))
        assertTrue(prompt.contains("FITNESS: 100%"))
        assertTrue(prompt.contains("HEALTH: 100%"))
    }

    @Test
    fun testStrugglingUser() {
        val streak = StreakEntity(id = 1, currentStreak = 0, longestStreak = 4, perfectDaysCount = 2)
        val profile = UserProfileEntity(id = 1, username = "TryingHero", avatarId = "mage_1", level = 2)
        val tasks = listOf(
            createTask(1, TaskCategory.STUDY),
            createTask(2, TaskCategory.FITNESS)
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = false, pointsAwarded = 0),
            TaskCompletionLogEntity(id = 2, taskId = 1, completedDate = LocalDate.now().minusDays(1), wasCompleted = false, pointsAwarded = 0),
            TaskCompletionLogEntity(id = 3, taskId = 2, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10),
            TaskCompletionLogEntity(id = 4, taskId = 2, completedDate = LocalDate.now().minusDays(1), wasCompleted = false, pointsAwarded = 0)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_HEROIC)

        assertTrue(prompt.contains("Current Streak = 0"))
        assertTrue(prompt.contains("25% completion rate (3 missed entries)"))
        assertTrue(prompt.contains("STUDY: 0%"))
        assertTrue(prompt.contains("FITNESS: 50%"))
    }
}
