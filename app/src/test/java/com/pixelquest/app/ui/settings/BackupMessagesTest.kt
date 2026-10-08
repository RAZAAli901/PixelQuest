package com.pixelquest.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.settings.BackupMessages
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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

/**
 * Picking a file that isn't a backup used to open the restore dialog anyway, and restoring then did
 * nothing, silently. Settings now says what happened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupMessagesTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val scheduler = TaskAlarmScheduler(context)
        val signals = LevelUpSignalManager(context)
        viewModel = SettingsViewModel(
            FakeSettingsRepository(), FakeUserProfileRepository(), FakeDifficultySettingsRepository(), FakeTaskRepository(),
            scheduler, ProgressReset(db, scheduler, signals), BackupRestorer(db, scheduler, signals)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun file(name: String, text: String): Uri =
        Uri.fromFile(File(context.cacheDir, name).apply { writeText(text) })

    private fun awaitMessage(): String? {
        repeat(50) { viewModel.backupMessage.value?.let { return it }; Thread.sleep(20) }
        return viewModel.backupMessage.value
    }

    @Test
    fun aFileThatIsntABackup_saysSo_andOpensNoDialog() {
        viewModel.onImportFileSelected(context, file("photo.json", """{"type": "something else"}"""))

        assertEquals(BackupMessages.NOT_A_BACKUP, awaitMessage())
        assertFalse(viewModel.showRestoreConfirmDialog.value)
    }

    @Test
    fun aBackup_opensTheDialog_andRestoringSaysSo() {
        val backup = """{"userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": []}"""
        viewModel.onImportFileSelected(context, file("backup.json", backup))
        repeat(50) { if (!viewModel.showRestoreConfirmDialog.value) Thread.sleep(20) }
        assertTrue(viewModel.showRestoreConfirmDialog.value)

        viewModel.confirmImport()

        assertEquals(BackupMessages.RESTORED, awaitMessage())
        assertFalse(viewModel.showRestoreConfirmDialog.value)
    }

    @Test
    fun savingABackup_saysSo() {
        val uri = Uri.parse("content://test.documents/backup.json")
        val written = java.io.ByteArrayOutputStream()
        org.robolectric.Shadows.shadowOf(context.contentResolver).registerOutputStream(uri, written)

        viewModel.exportBackupToUri(context, uri)

        assertEquals(BackupMessages.SAVED, awaitMessage())
        assertTrue(written.toString().contains("\"tasks\""))
    }

    @Test
    fun aSaveThatFails_saysSo() {
        // E.g. the storage is full: writing fails.
        val uri = Uri.parse("content://test.documents/full.json")
        org.robolectric.Shadows.shadowOf(context.contentResolver).registerOutputStream(uri, object : java.io.OutputStream() {
            override fun write(b: Int) = throw java.io.IOException("No space left on device")
            override fun write(b: ByteArray, off: Int, len: Int) = throw java.io.IOException("No space left on device")
        })

        viewModel.exportBackupToUri(context, uri)

        assertEquals(BackupMessages.SAVE_FAILED, awaitMessage())
    }
}
