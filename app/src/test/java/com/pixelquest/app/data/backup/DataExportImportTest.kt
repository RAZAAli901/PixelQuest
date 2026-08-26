package com.pixelquest.app.data.backup

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.Priority
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DataExportImportTest {

    @Test
    fun testExportAndImportRoundtripFidelity() {
        val originalProfile = UserProfileEntity(
            id = 1,
            username = "TestHero",
            avatarId = "avatar_mage",
            level = 4,
            totalXp = 450,
            perfectDaysTowardNextLevel = 2
        )

        val originalDifficulty = DifficultySettingsEntity(
            id = 1,
            difficultyLevel = DifficultyLevel.HARD,
            perfectDayThreshold = 0.9f,
            daysRequiredPerLevel = 10
        )

        val originalStreak = StreakEntity(
            id = 1,
            currentStreak = 7,
            longestStreak = 12,
            lastCompletedDate = LocalDate.of(2026, 8, 25)
        )

        val originalTask = TaskEntity(
            id = 101,
            name = "Morning Quest",
            description = "Slay the dragon",
            scheduledTime = LocalTime.of(8, 30),
            scheduledDay = LocalDate.of(2026, 8, 26),
            recurrenceType = RecurrenceType.DAILY,
            priority = Priority.HIGH,
            difficulty = TaskDifficulty.HARD,
            isCompleted = false
        )

        val originalPayload = BackupPayload(
            userProfile = originalProfile,
            difficultySettings = originalDifficulty,
            streak = originalStreak,
            tasks = listOf(originalTask)
        )

        val jsonString = DataExportImport.exportToJson(originalPayload)
        assertNotNull(jsonString)

        val restoredPayload = DataExportImport.importFromJson(jsonString)

        assertEquals("TestHero", restoredPayload.userProfile?.username)
        assertEquals("avatar_mage", restoredPayload.userProfile?.avatarId)
        assertEquals(DifficultyLevel.HARD, restoredPayload.difficultySettings?.difficultyLevel)
        assertEquals(7, restoredPayload.streak?.currentStreak)
        assertEquals(1, restoredPayload.tasks.size)
        assertEquals("Morning Quest", restoredPayload.tasks[0].name)
    }
}
