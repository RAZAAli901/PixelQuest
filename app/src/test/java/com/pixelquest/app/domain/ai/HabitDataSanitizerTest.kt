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
 * Step 21: Unit tests verifying that active sanitization in HabitInsightPromptBuilder
 * reliably excludes PII fields (emails, phone numbers, usernames, raw task titles)
 * even if accidentally passed into prompt construction.
 */
class HabitDataSanitizerTest {

    @Test
    fun testActiveSanitization_stripsEmailAddresses() {
        val input = "Adventurer email is test.user123@example.com and backup is hero@realm.org"
        val sanitized = HabitInsightPromptBuilder.sanitizePromptText(input)

        assertFalse("Raw email should not be present", sanitized.contains("test.user123@example.com"))
        assertFalse("Raw backup email should not be present", sanitized.contains("hero@realm.org"))
        assertTrue("Email should be replaced with redaction tag", sanitized.contains("[REDACTED_EMAIL]"))
    }

    @Test
    fun testActiveSanitization_stripsPhoneNumbers() {
        val input = "Emergency contact: +1 555-432-8765 or (555) 987-6543."
        val sanitized = HabitInsightPromptBuilder.sanitizePromptText(input)

        assertFalse("Phone number should be stripped", sanitized.contains("555-432-8765"))
        assertFalse("Formatted phone number should be stripped", sanitized.contains("(555) 987-6543"))
        assertTrue("Phone should be replaced with redaction tag", sanitized.contains("[REDACTED_PHONE]"))
    }

    @Test
    fun testActiveSanitization_excludesUsernameAndDisplayNamesEvenIfInjected() {
        val streak = StreakEntity(id = 1, currentStreak = 4, longestStreak = 10, perfectDaysCount = 6)
        val profile = UserProfileEntity(
            id = 1,
            username = "VerySecretRealName",
            avatarId = "wizard_1",
            level = 3,
            leaderboardDisplayName = "PublicGamerTag99"
        )
        val tasks = listOf(
            TaskEntity(
                id = 1,
                name = "VerySecretRealName Routine",
                description = "Routine for VerySecretRealName",
                scheduledDay = LocalDate.now(),
                scheduledTime = LocalTime.NOON,
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.FITNESS
            )
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.GAMIFIED_HEROIC)

        assertFalse("Prompt must not contain username", prompt.contains("VerySecretRealName"))
        assertFalse("Prompt must not contain display name", prompt.contains("PublicGamerTag99"))
        assertFalse("Prompt must not contain verbatim task description", prompt.contains("Routine for"))
    }

    @Test
    fun testActiveSanitization_excludesPersonalMedicalAndTherapyTaskTitles() {
        val sensitiveTaskTitle = "Take blood pressure medication"
        val sensitiveTaskDesc = "Refill prescription at local clinic"

        val streak = StreakEntity(id = 1, currentStreak = 3, longestStreak = 5, perfectDaysCount = 4)
        val profile = UserProfileEntity(id = 1, username = "JohnDoe", avatarId = "warrior_1", level = 2)
        val tasks = listOf(
            TaskEntity(
                id = 1,
                name = sensitiveTaskTitle,
                description = sensitiveTaskDesc,
                scheduledDay = LocalDate.now(),
                scheduledTime = LocalTime.of(8, 0),
                recurrenceType = RecurrenceType.DAILY,
                category = TaskCategory.HEALTH
            )
        )
        val logs = listOf(
            TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true, pointsAwarded = 10)
        )

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(streak, profile, tasks, logs)
        val prompt = HabitInsightPromptBuilder.buildPrompt(telemetry, HabitInsightTone.SIMPLE_MINIMALIST)

        assertFalse("Prompt must not contain sensitive task title", prompt.contains(sensitiveTaskTitle))
        assertFalse("Prompt must not contain sensitive task description", prompt.contains(sensitiveTaskDesc))
        assertFalse("Prompt must not contain user real name", prompt.contains("JohnDoe"))
        assertTrue("Prompt should safely contain category metrics", prompt.contains("HEALTH: 100%"))
    }

    @Test
    fun testActiveSanitization_preservesStandardCategoryNames() {
        val input = "HEALTH: 80% (4/5 completed) and FITNESS: 100% (2/2 completed)"
        val sanitized = HabitInsightPromptBuilder.sanitizePromptText(input)

        assertTrue(sanitized.contains("HEALTH: 80%"))
        assertTrue(sanitized.contains("FITNESS: 100%"))
    }
}
