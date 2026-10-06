package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** A backup brings back the streak, completion history and level timeline, and a restore keeps this install's cloud link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupRoundTripTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databases = mutableListOf<AppDatabase>()
    private val day = LocalDate.of(2026, 9, 20)

    private fun newDb() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries().build().also { databases += it }

    @After
    fun tearDown() = databases.forEach { it.close() }

    private fun task(id: Long, name: String) = TaskEntity(
        id = id, name = name, description = "", scheduledDay = day, scheduledTime = LocalTime.of(8, 0),
        recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
    )

    private suspend fun seedOldPhone(db: AppDatabase) {
        db.taskDao().insertTask(task(1, "Run"))
        db.taskDao().insertTask(task(2, "Read"))
        db.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = 1, completedDate = day, wasCompleted = true, pointsAwarded = 50))
        db.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = 2, completedDate = day, wasCompleted = false, pointsAwarded = 0))
        db.streakDao().insertStreak(StreakEntity(currentStreak = 6, longestStreak = 9, lastCompletedDate = day, perfectDaysCount = 21))
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Aly", avatarId = "avatar_mage", level = 3, totalXp = 1500))
    }

    private fun backupJson(db: AppDatabase) = runBlocking {
        DataExportImport.exportToJson(BackupRestorer(db, TaskAlarmScheduler(context)).snapshot())
    }

    @Test
    fun restore_bringsBackStreakTasksAndHistory_andKeepsTheCloudLink() = runBlocking {
        val oldPhone = newDb().also { seedOldPhone(it) }
        val json = backupJson(oldPhone)

        // The new install is signed in and on the leaderboard.
        val newPhone = newDb()
        newPhone.userProfileDao().insertProfile(
            UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero", supabaseUserId = "u1", leaderboardOptIn = true, leaderboardDisplayName = "Aly_Q")
        )
        BackupRestorer(newPhone, TaskAlarmScheduler(context)).restore(DataExportImport.importFromJson(json))

        assertEquals(StreakEntity(currentStreak = 6, longestStreak = 9, lastCompletedDate = day, perfectDaysCount = 21), newPhone.streakDao().getCurrentStreak().first())
        assertEquals(listOf("Read", "Run"), newPhone.taskDao().getAllTasks().first().map { it.name }.sorted())
        val logs = newPhone.taskCompletionLogDao().getAllLogs().first()
        assertEquals(setOf(1L to true, 2L to false), logs.map { it.taskId to it.wasCompleted }.toSet())

        val profile = newPhone.userProfileDao().getProfile().first()!!
        assertEquals("Aly", profile.username)
        assertEquals(1500, profile.totalXp)
        assertEquals("u1", profile.supabaseUserId)
        assertTrue(profile.leaderboardOptIn)
        assertEquals("Aly_Q", profile.leaderboardDisplayName)
    }

    @Test
    fun aBackupFromBeforeDay28_keepsTheCurrentHistory() = runBlocking {
        val oldPhone = newDb().also { seedOldPhone(it) }
        val legacy = JSONObject(backupJson(oldPhone)).apply { remove("logs") }.toString()

        val newPhone = newDb()
        newPhone.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = 1, completedDate = day.plusDays(1), wasCompleted = true, pointsAwarded = 50))
        val payload = DataExportImport.importFromJson(legacy)
        assertNull(payload.logs)

        BackupRestorer(newPhone, TaskAlarmScheduler(context)).restore(payload)

        assertEquals(1, newPhone.taskCompletionLogDao().getAllLogs().first().size)
    }

    @Test
    fun restore_bringsBackTheLevelTimeline() = runBlocking {
        val oldPhone = newDb().also { seedOldPhone(it) }
        oldPhone.levelHistoryDao().insertLevelHistory(LevelHistoryEntity(level = 2, achievedDate = 1_000L, difficultyAtTimeOfLevelUp = "EASY"))
        oldPhone.levelHistoryDao().insertLevelHistory(LevelHistoryEntity(level = 3, achievedDate = 2_000L, difficultyAtTimeOfLevelUp = "HARD"))
        val json = backupJson(oldPhone)

        val newPhone = newDb()
        newPhone.levelHistoryDao().insertLevelHistory(LevelHistoryEntity(level = 2, achievedDate = 9_999L, difficultyAtTimeOfLevelUp = "MEDIUM"))
        BackupRestorer(newPhone, TaskAlarmScheduler(context)).restore(DataExportImport.importFromJson(json))

        val timeline = newPhone.levelHistoryDao().getAllHistory().first().sortedBy { it.level }
        assertEquals(listOf(Triple(2, 1_000L, "EASY"), Triple(3, 2_000L, "HARD")), timeline.map { Triple(it.level, it.achievedDate, it.difficultyAtTimeOfLevelUp) })
    }

    @Test
    fun aBackupFromBeforeDay30_keepsTheCurrentLevelTimeline() = runBlocking {
        val oldPhone = newDb().also { seedOldPhone(it) }
        val legacy = JSONObject(backupJson(oldPhone)).apply { remove("levelHistory") }.toString()

        val newPhone = newDb()
        newPhone.levelHistoryDao().insertLevelHistory(LevelHistoryEntity(level = 2, achievedDate = 9_999L, difficultyAtTimeOfLevelUp = "MEDIUM"))
        val payload = DataExportImport.importFromJson(legacy)
        assertNull(payload.levelHistory)

        BackupRestorer(newPhone, TaskAlarmScheduler(context)).restore(payload)

        assertEquals(1, newPhone.levelHistoryDao().getAllHistory().first().size)
    }

    @Test
    fun aStreakThatWasNeverEvaluated_staysUnevaluated() = runBlocking {
        val db = newDb()
        db.streakDao().insertStreak(StreakEntity())
        db.taskDao().insertTask(task(1, "Run"))

        val restored = DataExportImport.importFromJson(backupJson(db)).streak!!
        assertNull(restored.lastCompletedDate) // used to come back as today
    }
}
