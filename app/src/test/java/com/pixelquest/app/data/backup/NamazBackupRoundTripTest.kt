package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
import java.io.File
import java.time.LocalDate

/**
 * The Namaz Tracker's prayer log and settings go through a backup file and back (format 5), and a
 * file from before the tracker leaves the phone's Namaz data as it is.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazBackupRoundTripTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var phone: AppDatabase
    private lateinit var newPhone: AppDatabase
    private val copyFile = File(context.filesDir, "namaz-test-" + RestoreSafetyCopy.FILE_NAME)
    private lateinit var restorer: BackupRestorer
    private lateinit var newPhoneRestorer: BackupRestorer

    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)

    private val settings = NamazSettingsEntity(
        enabled = true, method = "MUSLIM_WORLD_LEAGUE", asrMadhab = "SHAFI", highLatitudeRule = "SEVENTH_OF_THE_NIGHT",
        latitude = 24.8607, longitude = 67.0011, city = "Karachi",
        reminderOffsetMin = 10, remindAsr = false, adjustFajr = 2, adjustIsha = -3, xpEnabled = false
    )
    private val log = listOf(
        NamazLogEntity(date = yesterday, prayer = Prayer.FAJR, status = PrayerStatus.PRAYED, prayedAt = 1_760_000_000_000, xpAwarded = 10),
        NamazLogEntity(date = yesterday, prayer = Prayer.DHUHR, status = PrayerStatus.MISSED),
        NamazLogEntity(date = yesterday, prayer = Prayer.ISHA, status = PrayerStatus.PRAYED, prayedAt = 1_760_050_000_000, xpAwarded = 5),
        NamazLogEntity(date = today, prayer = Prayer.FAJR, status = PrayerStatus.PENDING)
    )

    private fun database() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    private suspend fun AppDatabase.namazLog() =
        namazLogDao().getAll().map { it.copy(id = 0) }.sortedWith(compareBy({ it.date }, { it.prayer.ordinal }))

    @Before
    fun setUp(): Unit = runBlocking {
        copyFile.delete()
        phone = database()
        newPhone = database()
        restorer = BackupRestorer(phone, TaskAlarmScheduler(context), LevelUpSignalManager(context), null, RestoreSafetyCopy(copyFile))
        newPhoneRestorer = BackupRestorer(newPhone, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        phone.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 900))
        log.forEach { phone.namazLogDao().upsert(it) }
        phone.namazSettingsDao().save(settings)
    }

    @After
    fun tearDown() {
        phone.close()
        newPhone.close()
        copyFile.delete()
    }

    @Test
    fun theNamazLogAndSettings_goThroughAFileAndBack() = runBlocking {
        val file = DataExportImport.exportToJson(restorer.snapshot())

        val read = DataExportImport.importFromJson(file)
        assertEquals(5, read.formatVersion)
        newPhoneRestorer.restore(read)

        assertEquals(log, newPhone.namazLog())
        assertEquals(settings, newPhone.namazSettingsDao().get())
    }

    @Test
    fun aPhoneThatNeverUsedTheTracker_backsUpAnEmptyLog_andNoSettings() = runBlocking {
        val payload = BackupRestorer(newPhone, TaskAlarmScheduler(context), LevelUpSignalManager(context)).snapshot()
        val json = JSONObject(DataExportImport.exportToJson(payload))

        assertEquals(0, json.getJSONArray("namazLog").length())
        assertFalse(json.has("namazSettings"))
        assertNull(DataExportImport.importFromJson(json.toString()).namazSettings)
    }

    @Test
    fun aFileFromBeforeTheTracker_leavesThePhonesNamazDataAsItIs() = runBlocking {
        restorer.restore(
            DataExportImport.importFromJson(
                """{"formatVersion": 4, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": []}"""
            )
        )

        assertEquals(log, phone.namazLog())
        assertEquals(settings, phone.namazSettingsDao().get())
    }

    @Test
    fun aFormat5File_replacesThePhonesLog_andTheRestoreCanBeUndone() = runBlocking {
        val theirs = """
            {"formatVersion": 5, "userProfile": {"id": 1, "username": "Other", "avatarId": "avatar_hero", "level": 1, "totalXp": 50}, "tasks": [],
             "namazLog": [{"date": "$today", "prayer": "DHUHR", "status": "PRAYED", "prayedAt": 1760100000000, "xpAwarded": 0}]}
        """.trimIndent()

        restorer.restore(DataExportImport.importFromJson(theirs))
        assertEquals(listOf(Prayer.DHUHR), phone.namazLog().map { it.prayer })
        // The file had no Namaz settings, so the phone's stay.
        assertEquals(settings, phone.namazSettingsDao().get())

        assertTrue(restorer.undoLastRestore())
        assertEquals(log, phone.namazLog())
    }

    @Test
    fun aPrayedPrayer_isProgressWorthBackingUp() {
        val prayed = BackupPayload(null, null, null, emptyList(), namazLogs = log.take(1))
        val onlyMissed = BackupPayload(null, null, null, emptyList(), namazLogs = log.drop(1).take(1))

        assertTrue(prayed.hasProgress)
        assertFalse(onlyMissed.hasProgress)
    }

    @Test
    fun importedNamazValues_areKeptInRange() {
        val tomorrow = today.plusDays(1)
        val read = DataExportImport.importFromJson(
            """
            {"formatVersion": 5, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": [],
             "namazSettings": {"enabled": true, "method": "NOT_A_METHOD", "latitude": 123.0, "longitude": 74.0, "city": "Nowhere",
                               "reminderOffsetMin": 9999, "adjustFajr": -500, "adjustIsha": 45},
             "namazLog": [
               {"date": "$yesterday", "prayer": "FAJR", "status": "PRAYED", "prayedAt": -5, "xpAwarded": 99999999},
               {"date": "$yesterday", "prayer": "FAJR", "status": "MISSED"},
               {"date": "$tomorrow", "prayer": "ASR", "status": "PRAYED"},
               {"date": "$yesterday", "prayer": "TAHAJJUD", "status": "PRAYED"},
               {"date": "not a date", "prayer": "ASR", "status": "PRAYED"}
             ]}
            """.trimIndent()
        )

        val logs = read.namazLogs!!
        assertEquals(1, logs.size)
        assertEquals(PrayerStatus.PRAYED, logs.single().status)
        assertNull(logs.single().prayedAt)
        assertEquals(10_000, logs.single().xpAwarded)

        val s = read.namazSettings!!
        assertEquals(NamazSettingsEntity.SINGLE_ROW_ID, s.id)
        assertNull(s.latitude)
        assertNull(s.longitude)
        assertEquals(120, s.reminderOffsetMin)
        assertEquals(-60, s.adjustFajr)
        assertEquals(45, s.adjustIsha)
        // Kept as written: the time engine falls back to Karachi's method for a name it doesn't know.
        assertEquals("NOT_A_METHOD", s.method)
    }
}
