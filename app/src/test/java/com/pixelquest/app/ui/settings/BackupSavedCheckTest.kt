package com.pixelquest.app.ui.settings

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A saved backup is read back and checked, and Settings says when the last good one was saved. A
 * storage provider can fail to save a file in full without saying so; the player would only find
 * out when restoring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupSavedCheckTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 900))
        val scheduler = TaskAlarmScheduler(context)
        val signals = LevelUpSignalManager(context)
        viewModel = SettingsViewModel(
            settings, FakeUserProfileRepository(), FakeDifficultySettingsRepository(), FakeTaskRepository(),
            scheduler, ProgressReset(db, scheduler, signals), BackupRestorer(db, scheduler, signals)
        )
    }

    @After
    fun tearDown() {
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    private fun awaitMessage(): String? {
        repeat(100) { viewModel.backupMessage.value?.let { return it }; Thread.sleep(20) }
        return viewModel.backupMessage.value
    }

    @Test
    fun aSavedBackup_isCheckedByReadingItBack_andRemembered() {
        // A document provider that keeps what it's given and hands it back.
        val uri = Uri.parse("content://test.documents/pixelquest-backup.json")
        val saved = java.io.ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(uri, saved)
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { saved.toByteArray().inputStream() }

        viewModel.exportBackupToUri(context, uri)

        assertEquals(BackupMessages.SAVED, awaitMessage())
        assertTrue(saved.toString().contains("\"Aria\""))
        assertTrue("the time is kept", settings.lastBackupAt.value > 0L)
    }

    @Test
    fun aFileThatReadsBackIncomplete_saysSo_andIsntCountedAsABackup() {
        val uri = Uri.parse("content://test.documents/broken.json")
        shadowOf(context.contentResolver).registerOutputStream(uri, java.io.ByteArrayOutputStream())
        // What the provider handed back: the file cut short.
        shadowOf(context.contentResolver).registerInputStream(uri, "{\"userProfile\": {".byteInputStream())

        viewModel.exportBackupToUri(context, uri)

        assertEquals(BackupMessages.SAVE_NOT_CHECKED, awaitMessage())
        assertEquals(0L, settings.lastBackupAt.value)
    }

    @Test
    fun aProviderThatCantReadBack_stillSaves() {
        val uri = Uri.parse("content://test.documents/write-only.json")
        shadowOf(context.contentResolver).registerOutputStream(uri, java.io.ByteArrayOutputStream())

        viewModel.exportBackupToUri(context, uri)

        assertEquals(BackupMessages.SAVED, awaitMessage())
    }

    @Test
    fun settingsSaysWhenTheLastBackupWas() = runBlocking {
        assertEquals("No backup saved yet.", viewModel.lastBackupText.first { it.isNotEmpty() })
        settings.lastBackupAt.value = ZonedDateTime.of(2026, 10, 12, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals("Last backup: 12 Oct 2026", viewModel.lastBackupText.first { it.startsWith("Last") })
    }
}
