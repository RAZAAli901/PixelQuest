package com.pixelquest.app.ui.settings

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Settings offers UNDO LAST RESTORE after a restore, and says when it has put everything back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UndoRestoreSettingsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val copyFile = File(context.filesDir, "settings-undo-" + RestoreSafetyCopy.FILE_NAME)
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp(): Unit = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        copyFile.delete()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 6, totalXp = 3000))
        val scheduler = TaskAlarmScheduler(context)
        val signals = LevelUpSignalManager(context)
        viewModel = SettingsViewModel(
            FakeSettingsRepository(), FakeUserProfileRepository(), FakeDifficultySettingsRepository(), FakeTaskRepository(),
            scheduler, ProgressReset(db, scheduler, signals), BackupRestorer(db, scheduler, signals, null, RestoreSafetyCopy(copyFile))
        )
    }

    @After
    fun tearDown() {
        db.closeAfterWrites()
        Dispatchers.resetMain()
        copyFile.delete()
    }

    private fun awaitMessage(expected: String): String? {
        repeat(100) { if (viewModel.backupMessage.value == expected) return expected; Thread.sleep(20) }
        return viewModel.backupMessage.value
    }

    @Test
    fun afterARestore_settingsOffersToUndoIt() = runBlocking {
        assertFalse(viewModel.canUndoRestore.value)
        val backup = File(context.cacheDir, "other.json").apply {
            writeText("""{"userProfile": {"id": 1, "username": "Other", "avatarId": "avatar_hero", "level": 1, "totalXp": 50}, "tasks": []}""")
        }
        viewModel.onImportFileSelected(context, Uri.fromFile(backup))
        repeat(50) { if (!viewModel.showRestoreConfirmDialog.value) Thread.sleep(20) }
        viewModel.confirmImport()
        assertEquals(BackupMessages.RESTORED, awaitMessage(BackupMessages.RESTORED))
        assertTrue(viewModel.canUndoRestore.value)

        viewModel.undoLastRestore()

        assertEquals(BackupMessages.RESTORE_UNDONE, awaitMessage(BackupMessages.RESTORE_UNDONE))
        assertEquals("Aria", db.userProfileDao().getProfile().first()!!.username)
        assertFalse(viewModel.canUndoRestore.value)
    }
}
