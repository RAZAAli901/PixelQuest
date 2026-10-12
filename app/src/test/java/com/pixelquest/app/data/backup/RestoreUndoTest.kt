package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * A restore can be undone. Restoring the wrong file (someone else's, or an old one) used to replace
 * the player's progress for good; what the phone had just before is now kept until it's used or the
 * next restore replaces it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RestoreUndoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val copyFile = File(context.filesDir, "undo-test-" + RestoreSafetyCopy.FILE_NAME)
    private val safetyCopy = RestoreSafetyCopy(copyFile)
    private lateinit var restorer: BackupRestorer

    @Before
    fun setUp(): Unit = runBlocking {
        copyFile.delete()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context), null, safetyCopy)
        // The player's own game.
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 6, totalXp = 3000))
        db.taskDao().insertTask(
            TaskEntity(id = 1, name = "Run", description = "", scheduledDay = LocalDate.of(2026, 10, 1), scheduledTime = LocalTime.of(7, 0),
                recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS)
        )
        db.taskCompletionLogDao().insertLog(TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.of(2026, 10, 11), wasCompleted = true, pointsAwarded = 50))
        Unit
    }

    @After
    fun tearDown() {
        db.close()
        copyFile.delete()
    }

    private val someoneElsesBackup = """
        {"formatVersion": 4, "userProfile": {"id": 1, "username": "Other", "avatarId": "avatar_hero", "level": 1, "totalXp": 50},
         "tasks": [{"id": 7, "name": "Their quest", "scheduledTime": "09:00", "scheduledDay": "2026-10-01", "recurrenceType": "DAILY", "category": "OTHER"}],
         "logs": []}
    """.trimIndent()

    @Test
    fun aRestore_canBeUndone_andEverythingComesBack() = runBlocking {
        assertFalse(restorer.canUndoLastRestore())

        restorer.restore(DataExportImport.importFromJson(someoneElsesBackup))
        assertEquals("Other", db.userProfileDao().getProfile().first()!!.username)
        assertTrue(restorer.canUndoLastRestore())

        assertTrue(restorer.undoLastRestore())

        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals("Aria", profile.username)
        assertEquals(6, profile.level)
        assertEquals(3000, profile.totalXp)
        assertEquals(listOf("Run"), db.taskDao().getAllTasks().first().map { it.name })
        assertEquals(1, db.taskCompletionLogDao().getAllLogs().first().size)
        assertFalse("the copy is used up", restorer.canUndoLastRestore())
    }

    @Test
    fun nothingRestored_nothingToUndo() = runBlocking {
        assertFalse(restorer.undoLastRestore())
        assertEquals("Aria", db.userProfileDao().getProfile().first()!!.username)
    }

    @Test
    fun theNextRestore_replacesTheCopy() = runBlocking {
        restorer.restore(DataExportImport.importFromJson(someoneElsesBackup))
        restorer.restore(DataExportImport.importFromJson(someoneElsesBackup.replace("Other", "Third")))

        restorer.undoLastRestore()

        assertEquals("one step back, to the first restore's result", "Other", db.userProfileDao().getProfile().first()!!.username)
    }
}
