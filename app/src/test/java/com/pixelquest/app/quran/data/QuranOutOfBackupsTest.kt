package com.pixelquest.app.quran.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupPayload
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.backup.DataExportImport
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Quran is the app's own text, not the player's progress (docs/quran-data.md): it never goes into
 * the game's backup file or the synced profile, and resetting or restoring the game leaves it be.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuranOutOfBackupsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @Before
    fun load() = runBlocking {
        QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        Unit
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun mentionsTheQuran(name: String) = listOf("quran", "ayah", "surah", "translation", "bismillah").any { it in name.lowercase() }

    @Test
    fun theGamesBackupFile_hasNoQuranData() = runBlocking {
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        val json = JSONObject(DataExportImport.exportToJson(restorer.snapshot()))

        val keys = json.keys().asSequence().toList()
        assertTrue(keys.toString(), keys.none(::mentionsTheQuran))
        assertTrue(BackupPayload::class.java.declaredFields.none { mentionsTheQuran(it.name) })
    }

    @Test
    fun theSyncedProfile_hasNothingOfTheQuran() {
        val dto = CloudProfileDto(id = "user", displayName = "Aria", currentStreak = 3, longestStreak = 9, level = 4, totalXp = 1530)
        val keys = Json.parseToJsonElement(Json.encodeToString(dto)).jsonObject.keys

        assertTrue(keys.toString(), keys.none(::mentionsTheQuran))
    }

    @Test
    fun resettingAllProgress_keepsTheQuran() = runBlocking {
        ProgressReset(db, TaskAlarmScheduler(context), LevelUpSignalManager(context)).resetAll()

        assertEquals(114, db.quranDao().surahCount())
        assertEquals(6236, db.quranDao().ayahCount())
    }

    @Test
    fun restoringABackup_keepsTheQuran() = runBlocking {
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        restorer.restore(BackupPayload(null, null, null, emptyList()))

        assertEquals(114, db.quranDao().surahCount())
        assertEquals(6236, db.quranDao().ayahCount())
        assertEquals(6236, db.quranDao().ayahCountIn("en.sahih"))
    }
}
