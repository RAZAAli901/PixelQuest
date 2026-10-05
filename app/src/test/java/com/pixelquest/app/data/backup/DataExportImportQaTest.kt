package com.pixelquest.app.data.backup

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// org.json is a stub on the plain JVM; Robolectric provides the real implementation.
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
class DataExportImportQaTest {

    @Test
    fun testManualQaExportAlterRestoreCycle() {
        val initialProfile = UserProfileEntity(1, "OriginalHero", "avatar_hero", 2, 150, 1)
        val initialDifficulty = DifficultySettingsEntity(1, DifficultyLevel.MEDIUM, 0.7f, 7)
        val initialStreak = StreakEntity(1, 3, 5, LocalDate.now())
        val initialTask = TaskEntity(id = 1, name = "Original Task", description = "Desc", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.now(), recurrenceType = RecurrenceType.DAILY, category = com.pixelquest.app.domain.model.TaskCategory.FITNESS)

        val backup = BackupPayload(initialProfile, initialDifficulty, initialStreak, listOf(initialTask))
        val exportedJson = DataExportImport.exportToJson(backup)

        // Simulate state alteration
        var currentProfile = UserProfileEntity(1, "AlteredHero", "avatar_ninja", 10, 2000, 5)
        var currentTasks = listOf(TaskEntity(id = 2, name = "Altered Task", description = "Desc", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.now(), recurrenceType = RecurrenceType.DAILY, category = com.pixelquest.app.domain.model.TaskCategory.CHORES))

        // Restore backup
        val restored = DataExportImport.importFromJson(exportedJson)
        currentProfile = restored.userProfile ?: currentProfile
        currentTasks = restored.tasks

        assertEquals("OriginalHero", currentProfile.username)
        assertEquals("avatar_hero", currentProfile.avatarId)
        assertEquals(1, currentTasks.size)
        assertEquals("Original Task", currentTasks[0].name)
    }
}
