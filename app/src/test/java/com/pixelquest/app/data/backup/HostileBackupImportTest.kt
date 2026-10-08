package com.pixelquest.app.data.backup

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * A backup is a file anyone can edit. Out-of-range values used to crash the app on every boot (a quest
 * dated in the year 999999999 overflowed the alarm time), hang Analytics, turn XP negative, freeze
 * streaks or restore nothing at all. They are now brought into range on import.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HostileBackupImportTest {

    private val hostile = """
        {
          "userProfile": {"id": 2, "username": "X", "avatarId": "avatar_hero", "level": -4, "totalXp": 2147483647, "perfectDaysTowardNextLevel": -1},
          "difficultySettings": {"id": 7, "difficultyLevel": "HARD"},
          "streak": {"id": 3, "currentStreak": 214748365, "longestStreak": -2, "lastCompletedDate": "2999-01-01", "perfectDaysCount": 5},
          "tasks": [
            {"id": 1, "name": "Far future", "scheduledDay": "+999999999-01-01", "scheduledTime": "09:00", "recurrenceType": "DAILY", "category": "FITNESS"},
            {"id": 1, "name": "Duplicate id", "scheduledDay": "2026-10-01", "scheduledTime": "09:00", "recurrenceType": "DAILY", "category": "FITNESS"},
            {"id": 2, "name": "Ancient", "scheduledDay": "-999999999-01-01", "scheduledTime": "09:00", "recurrenceType": "DAILY", "category": "FITNESS"},
            {"id": -5, "name": "Bad id", "scheduledDay": "2026-10-01", "scheduledTime": "09:00", "recurrenceType": "DAILY", "category": "FITNESS"}
          ],
          "logs": [
            {"taskId": 1, "completedDate": "2026-10-02", "wasCompleted": true, "pointsAwarded": 99999999},
            {"taskId": 1, "completedDate": "2999-01-01", "wasCompleted": true, "pointsAwarded": 50}
          ],
          "levelHistory": [
            {"level": 5000, "achievedDate": 1759900000000, "difficulty": "HARD"},
            {"level": 2, "achievedDate": 99999999999999, "difficulty": "HARD"}
          ]
        }
    """.trimIndent()

    private val today = LocalDate.now()

    @Test
    fun everyValue_isBroughtIntoRange() {
        val payload = DataExportImport.importFromJson(hostile)

        val profile = payload.userProfile!!
        assertEquals("The app only reads row 1", 1L, profile.id)
        assertEquals(1, profile.level)
        assertEquals(10_000_000, profile.totalXp)
        assertEquals(0, profile.perfectDaysTowardNextLevel)
        assertEquals(1L, payload.difficultySettings!!.id)

        val streak = payload.streak!!
        assertEquals(1L, streak.id)
        assertEquals(36_500, streak.currentStreak)
        assertEquals(0, streak.longestStreak)
        assertNull("A future 'last evaluated' day would freeze streaks", streak.lastCompletedDate)

        assertEquals("Duplicate and non-positive ids are dropped", listOf(1L, 2L), payload.tasks.map { it.id })
        assertEquals("Far future", payload.tasks[0].name)
        assertEquals(today, payload.tasks[0].scheduledDay)
        assertEquals(today, payload.tasks[1].scheduledDay)

        val logs = payload.logs!!
        assertEquals("Future logs are dropped", 1, logs.size)
        assertEquals(10_000, logs.single().pointsAwarded)

        val history = payload.levelHistory!!
        assertEquals(1, history.size)
        assertEquals(999, history.single().level)
    }

    @Test
    fun aNormalBackup_isUnchanged() {
        val normal = """
            {"userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 4, "totalXp": 1200, "perfectDaysTowardNextLevel": 3},
             "streak": {"id": 1, "currentStreak": 6, "longestStreak": 9, "lastCompletedDate": "${today.minusDays(1)}", "perfectDaysCount": 21},
             "tasks": [{"id": 3, "name": "Run", "scheduledDay": "2026-09-01", "scheduledTime": "07:00", "recurrenceType": "DAILY", "category": "FITNESS"}],
             "logs": [{"taskId": 3, "completedDate": "2026-09-02", "wasCompleted": true, "pointsAwarded": 50}]}
        """.trimIndent()

        val payload = DataExportImport.importFromJson(normal)

        assertEquals(4, payload.userProfile!!.level)
        assertEquals(1200, payload.userProfile!!.totalXp)
        assertEquals(today.minusDays(1), payload.streak!!.lastCompletedDate)
        assertEquals(LocalDate.of(2026, 9, 1), payload.tasks.single().scheduledDay)
        assertEquals(50, payload.logs!!.single().pointsAwarded)
    }
}
