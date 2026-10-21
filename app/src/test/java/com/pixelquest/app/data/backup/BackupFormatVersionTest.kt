package com.pixelquest.app.data.backup

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.BuildConfig
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
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Backups say which format they are and which app wrote them, so a later version knows what it's
 * reading. A backup from a newer app (restored onto an older phone, say) restores what this version
 * knows, and Settings says the rest was left out instead of just "Backup restored."
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupFormatVersionTest {

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
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    @Test
    fun aBackup_saysItsFormat_andTheAppThatWroteIt() {
        val json = JSONObject(DataExportImport.exportToJson(BackupPayload(null, null, null, emptyList())))
        assertEquals(DataExportImport.FORMAT_VERSION, json.getInt("formatVersion"))
        assertEquals(BuildConfig.VERSION_NAME, json.getString("appVersion"))
    }

    @Test
    fun theFormatIsReadBack_andOldFilesReadAsZero() {
        val profile = """"userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}"""
        assertEquals(5, DataExportImport.importFromJson("""{"formatVersion": 5, $profile, "tasks": []}""").formatVersion)
        assertEquals(0, DataExportImport.importFromJson("""{$profile, "tasks": []}""").formatVersion)
    }

    private fun restoreFile(text: String): String? {
        viewModel.onImportFileSelected(context, Uri.fromFile(File(context.cacheDir, "backup.json").apply { writeText(text) }))
        repeat(50) { if (!viewModel.showRestoreConfirmDialog.value) Thread.sleep(20) }
        viewModel.confirmImport()
        repeat(50) { viewModel.backupMessage.value?.let { return it }; Thread.sleep(20) }
        return viewModel.backupMessage.value
    }

    @Test
    fun aBackupFromANewerApp_restores_andSaysWhatThatMeans() {
        val newer = DataExportImport.FORMAT_VERSION + 1
        val message = restoreFile("""{"formatVersion": $newer, "aFutureSection": {"x": 1}, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": []}""")
        assertEquals(BackupMessages.RESTORED_FROM_NEWER, message)
    }

    @Test
    fun aBackupFromThisVersionOrOlder_justRestores() {
        assertEquals(
            BackupMessages.RESTORED,
            restoreFile("""{"userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": []}""")
        )
    }
}
