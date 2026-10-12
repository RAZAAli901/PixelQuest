package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.SeedDataProvider
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.TaskOccurrence
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * A backup made by v1.1.0 restores fully into today's app on a fresh install: the upgrade path for
 * 1.1.0 players (CHANGELOG, RELEASE_SIGNING.md) is export, uninstall, reinstall, import. The file is
 * exactly what v1.1.0's DataExportImport.exportToJson wrote: no logs, level history, weekdays,
 * reminder settings or perfect-day count, and a streak date of "null" when there was none.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OldBackupRestoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        // A fresh install, as the seed leaves it.
        SeedDataProvider.initialTasks().forEach { db.taskDao().insertTask(it) }
    }

    @After
    fun tearDown() = db.close()

    private fun v110Backup(lastCompletedDate: String) = """
        {
          "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_knight", "level": 5, "totalXp": 2400, "perfectDaysTowardNextLevel": 3},
          "difficultySettings": {"id": 1, "difficultyLevel": "HARD", "perfectDayThreshold": 0.9, "daysRequiredPerLevel": 14},
          "streak": {"id": 1, "currentStreak": 4, "longestStreak": 11, "lastCompletedDate": "$lastCompletedDate"},
          "tasks": [
            {"id": 1, "name": "Meditate", "description": "10 minutes", "scheduledTime": "07:30", "scheduledDay": "2026-09-01", "recurrenceType": "DAILY", "category": "HEALTH", "isActive": true},
            {"id": 2, "name": "Long run", "description": "", "scheduledTime": "18:00", "scheduledDay": "2026-09-09", "recurrenceType": "WEEKLY", "category": "FITNESS", "isActive": true},
            {"id": 3, "name": "Dentist", "description": "", "scheduledTime": "10:00", "scheduledDay": "2026-09-20", "recurrenceType": "ONE_TIME", "category": "OTHER", "isActive": true},
            {"id": 4, "name": "Old habit", "description": "", "scheduledTime": "12:00", "scheduledDay": "2026-09-02", "recurrenceType": "DAILY", "category": "CHORES", "isActive": false}
          ]
        }
    """.trimIndent()

    private suspend fun restore(json: String) {
        val payload = DataExportImport.importFromJson(json)
        assertTrue("a 1.1.0 backup is recognised", payload.isUsable)
        BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context)).restore(payload)
    }

    @Test
    fun everythingA110BackupHeld_comesBack() = runBlocking {
        restore(v110Backup("2026-09-30"))

        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals("Aria", profile.username)
        assertEquals("avatar_knight", profile.avatarId)
        assertEquals(5, profile.level)
        assertEquals(2400, profile.totalXp)
        assertEquals(3, profile.perfectDaysTowardNextLevel)

        assertEquals(DifficultyLevel.HARD, db.difficultySettingsDao().getCurrentDifficulty().first()!!.difficultyLevel)

        val streak = db.streakDao().getCurrentStreak().first()!!
        assertEquals(4, streak.currentStreak)
        assertEquals(11, streak.longestStreak)
        assertEquals(LocalDate.of(2026, 9, 30), streak.lastCompletedDate)

        val tasks = db.taskDao().getAllTasks().first().associateBy { it.name }
        assertEquals("the backup's quests replace the starter ones", setOf("Meditate", "Long run", "Dentist", "Old habit"), tasks.keys)
        val run = tasks.getValue("Long run")
        // 9 September 2026 was a Wednesday: the weekly quest keeps that weekday.
        assertTrue(TaskOccurrence.occursOn(run.scheduledDay, run.recurrenceType, LocalDate.of(2026, 10, 14), run.weeklyDays))
        assertFalse(TaskOccurrence.occursOn(run.scheduledDay, run.recurrenceType, LocalDate.of(2026, 10, 15), run.weeklyDays))
        assertTrue("reminders on, as 1.1.0 had them", tasks.values.all { it.reminderEnabled && it.reminderLeadMinutes == 0 && it.reminderStyle == ReminderStyle.STANDARD })
        assertFalse(tasks.getValue("Old habit").isActive)
    }

    @Test
    fun aStreakThatWasNeverSet_isWrittenAsTheWordNull_andRestoresAsNone() = runBlocking {
        restore(v110Backup("null"))
        assertNull(db.streakDao().getCurrentStreak().first()!!.lastCompletedDate)
    }

    @Test
    fun resultsOnTheStarterQuests_dontLandOnTheBackupsQuests() = runBlocking {
        // Ticked off a starter quest on the fresh install before importing. Its id (1) is the
        // backup's "Meditate", which must not inherit that result.
        db.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 10, 12), wasCompleted = true, pointsAwarded = 50))

        restore(v110Backup("2026-09-30"))

        assertEquals(emptyList<TaskCompletionLogEntity>(), db.taskCompletionLogDao().getAllLogs().first())
    }

    @Test
    fun restoringThisPhonesOwnOlderBackup_keepsTheHistoryOfTheSameQuests() = runBlocking {
        // The phone already has the backup's quests (same ids and names), with results.
        db.taskDao().deleteAll()
        restore(v110Backup("2026-09-30"))
        db.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 10, 11), wasCompleted = true, pointsAwarded = 50))

        restore(v110Backup("2026-09-30"))

        assertEquals(1, db.taskCompletionLogDao().getAllLogs().first().size)
    }

    @Test
    fun aV1_0_0Backup_restoresItsQuestsToo() = runBlocking {
        // v1.0.0 wrote priority, difficulty and isCompleted for a quest, and no category or isActive.
        restore("""
            {
              "userProfile": {"id": 1, "username": "Early", "avatarId": "avatar_hero", "level": 2, "totalXp": 300, "perfectDaysTowardNextLevel": 1},
              "streak": {"id": 1, "currentStreak": 1, "longestStreak": 2, "lastCompletedDate": "2026-08-30"},
              "tasks": [
                {"id": 1, "name": "Stretch", "description": "", "scheduledTime": "08:00", "scheduledDay": "2026-08-25", "recurrenceType": "DAILY", "priority": "HIGH", "difficulty": "MEDIUM", "isCompleted": false}
              ]
            }
        """.trimIndent())

        val stretch = db.taskDao().getAllTasks().first().single()
        assertEquals("Stretch", stretch.name)
        assertTrue("active, as every v1.0.0 quest was", stretch.isActive)
        assertEquals(2, db.userProfileDao().getProfile().first()!!.level)
    }
}

