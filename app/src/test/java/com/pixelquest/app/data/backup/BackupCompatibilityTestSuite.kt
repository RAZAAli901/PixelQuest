package com.pixelquest.app.data.backup

import android.app.Application
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Exhaustive version compatibility test suite for backup and export formats (Q044).
 * Verifies compatibility from unversioned format 0 up to current format 5 and future versions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupCompatibilityTestSuite {

    @Test
    fun formatVersion0_unversionedLegacy_parsesWithDefaults() {
        val legacyJson = """
            {
              "userProfile": {
                "id": 1,
                "username": "LegacyHero",
                "avatarId": "avatar_hero",
                "level": 3,
                "totalXp": 900
              },
              "streak": {
                "id": 1,
                "currentStreak": 2,
                "longestStreak": 5,
                "lastCompletedDate": "2026-09-01"
              },
              "tasks": [
                {
                  "id": 1,
                  "name": "LegacyTask",
                  "description": "Legacy description",
                  "scheduledTime": "09:00",
                  "scheduledDay": "2026-09-01",
                  "recurrenceType": "DAILY",
                  "category": "FITNESS",
                  "isActive": true
                }
              ]
            }
        """.trimIndent()

        val payload = DataExportImport.importFromJson(legacyJson)
        assertEquals(0, payload.formatVersion)
        assertNotNull(payload.userProfile)
        assertEquals("LegacyHero", payload.userProfile?.username)
        assertEquals(3, payload.userProfile?.level)
        assertEquals(1, payload.tasks.size)
        assertEquals("LegacyTask", payload.tasks.first().name)
        assertNull(payload.settings)
        assertNull(payload.namazLogs)
        assertTrue(payload.isUsable)
    }

    @Test
    fun formatVersion3_withAppVersionHeader_parsesCorrectly() {
        val v3Json = """
            {
              "formatVersion": 3,
              "appVersion": "1.5.0",
              "userProfile": {
                "id": 1,
                "username": "V3Hero",
                "avatarId": "avatar_mage",
                "level": 4,
                "totalXp": 1600
              },
              "tasks": []
            }
        """.trimIndent()

        val payload = DataExportImport.importFromJson(v3Json)
        assertEquals(3, payload.formatVersion)
        assertEquals("1.5.0", payload.appVersion)
        assertEquals("V3Hero", payload.userProfile?.username)
        assertTrue(payload.isUsable)
    }

    @Test
    fun formatVersion4_withSettings_parsesSettingsCorrectly() {
        val v4Json = """
            {
              "formatVersion": 4,
              "appVersion": "1.6.0",
              "userProfile": {
                "id": 1,
                "username": "V4Hero",
                "avatarId": "avatar_rogue",
                "level": 5,
                "totalXp": 2500
              },
              "settings": {
                "sound": false,
                "crtEffect": true,
                "haptics": false,
                "reduceMotion": true,
                "notifications": true,
                "theme": "PIXEL",
                "simpleMode": true
              },
              "tasks": []
            }
        """.trimIndent()

        val payload = DataExportImport.importFromJson(v4Json)
        assertEquals(4, payload.formatVersion)
        assertEquals("1.6.0", payload.appVersion)
        assertNotNull(payload.settings)
        assertEquals(false, payload.settings?.sound)
        assertEquals(true, payload.settings?.crtEffect)
        assertEquals(true, payload.settings?.reduceMotion)
        assertEquals("PIXEL", payload.settings?.theme)
        assertEquals(true, payload.settings?.simpleMode)
    }

    @Test
    fun formatVersion5_currentFormat_roundTripsAllSections() {
        val profile = UserProfileEntity(
            id = 1,
            username = "V5Hero",
            avatarId = "avatar_paladin",
            level = 8,
            totalXp = 6400,
            perfectDaysTowardNextLevel = 4,
        )
        val difficulty = DifficultySettingsEntity(
            id = 1,
            difficultyLevel = DifficultyLevel.HARD,
            perfectDayThreshold = 0.8f,
            daysRequiredPerLevel = 10,
        )
        val streak = StreakEntity(
            id = 1,
            currentStreak = 10,
            longestStreak = 25,
            lastCompletedDate = LocalDate.of(2026, 10, 23),
        )
        val task = TaskEntity(
            id = 101,
            name = "Read Quran",
            description = "1 Juz",
            scheduledDay = LocalDate.of(2026, 10, 23),
            scheduledTime = LocalTime.of(6, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING,
        )

        val originalPayload = BackupPayload(
            userProfile = profile,
            difficultySettings = difficulty,
            streak = streak,
            tasks = listOf(task),
            settings = BackupSettings(sound = true, theme = "COMIC"),
            namazLogs = emptyList(),
            namazSettings = null,
        )

        val exportedJson = DataExportImport.exportToJson(originalPayload)
        val importedPayload = DataExportImport.importFromJson(exportedJson)

        assertEquals(DataExportImport.FORMAT_VERSION, importedPayload.formatVersion)
        assertEquals(profile.username, importedPayload.userProfile?.username)
        assertEquals(difficulty.difficultyLevel, importedPayload.difficultySettings?.difficultyLevel)
        assertEquals(streak.currentStreak, importedPayload.streak?.currentStreak)
        assertEquals(1, importedPayload.tasks.size)
        assertEquals(task.name, importedPayload.tasks.first().name)
        assertEquals(true, importedPayload.settings?.sound)
        assertEquals("COMIC", importedPayload.settings?.theme)
    }

    @Test
    fun formatFutureVersion_withUnknownFields_gracefullyParsesKnownFields() {
        val futureJson = """
            {
              "formatVersion": 99,
              "appVersion": "9.9.0",
              "userProfile": {
                "id": 1,
                "username": "FutureHero",
                "avatarId": "avatar_hero",
                "level": 99,
                "totalXp": 999999
              },
              "unknownFutureSection": {
                "quantumQuests": [1, 2, 3],
                "hologramSync": true
              },
              "tasks": []
            }
        """.trimIndent()

        val payload = DataExportImport.importFromJson(futureJson)
        assertEquals(99, payload.formatVersion)
        assertEquals("9.9.0", payload.appVersion)
        assertEquals("FutureHero", payload.userProfile?.username)
        assertEquals(99, payload.userProfile?.level)
        assertTrue(payload.isUsable)
    }
}
