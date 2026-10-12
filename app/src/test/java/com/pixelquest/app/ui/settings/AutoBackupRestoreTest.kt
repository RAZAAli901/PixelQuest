package com.pixelquest.app.ui.settings

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.AutoBackups
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.backup.RestoreSafetyCopy
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.settings.BackupMessages
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** Settings lists the automatic backups kept on the phone and restores one like a file: confirmed, summarised, undoable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AutoBackupRestoreTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val dir = File(context.filesDir, "auto-restore-test")
    private val copyFile = File(context.filesDir, "auto-restore-" + RestoreSafetyCopy.FILE_NAME)
    private lateinit var db: AppDatabase
    private lateinit var viewModel: SettingsViewModel
    private val day = LocalDate.of(2026, 10, 11)

    @Before
    fun setUp(): Unit = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dir.deleteRecursively()
        copyFile.delete()
        AutoBackups(dir).save(
            day,
            """{"formatVersion": 4, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 4, "totalXp": 1800}, "tasks": []}"""
        )
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        // What the phone has now: the progress lost to a corrupt database, say.
        db.userProfileDao().insertProfile(UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero"))
        val scheduler = TaskAlarmScheduler(context)
        val signals = LevelUpSignalManager(context)
        viewModel = SettingsViewModel(
            FakeSettingsRepository(), FakeUserProfileRepository(), FakeDifficultySettingsRepository(), FakeTaskRepository(),
            scheduler, ProgressReset(db, scheduler, signals), BackupRestorer(db, scheduler, signals, null, RestoreSafetyCopy(copyFile)),
            autoBackups = AutoBackups(dir)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        dir.deleteRecursively()
        copyFile.delete()
    }

    @Test
    fun anAutomaticBackup_restoresLikeAFile() = runBlocking {
        assertEquals(listOf(day), viewModel.autoBackupDays.value)

        viewModel.onAutoBackupSelected(day)
        assertTrue(viewModel.showRestoreConfirmDialog.value)
        assertTrue(viewModel.pendingImportSummary.value!!.startsWith("Aria, level 4"))

        viewModel.confirmImport()
        repeat(100) { if (viewModel.backupMessage.value == null) Thread.sleep(20) }

        assertEquals(BackupMessages.RESTORED, viewModel.backupMessage.value)
        assertEquals(1800, db.userProfileDao().getProfile().first()!!.totalXp)
        assertTrue("and it can be undone", viewModel.canUndoRestore.value)
    }

    @Test
    fun aDayWithoutABackup_saysItCouldntBeRead() {
        viewModel.onAutoBackupSelected(day.minusDays(5))
        assertEquals(BackupMessages.READ_FAILED, viewModel.backupMessage.value)
    }
}
