package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A backup carries the app's settings (format 4), so export, reinstall and import, the upgrade path
 * for 1.1.0 players, no longer brings the progress back with every setting at its default.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var oldPhone: AppDatabase
    private lateinit var newPhone: AppDatabase

    @Before
    fun setUp() = runBlocking {
        oldPhone = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        newPhone = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        oldPhone.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 900))
    }

    @After
    fun tearDown() {
        oldPhone.close()
        newPhone.close()
    }

    private fun restorer(db: AppDatabase, settings: FakeSettingsRepository) =
        BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context), settings)

    private fun chosen() = FakeSettingsRepository(theme = ThemeMode.Comic, simpleMode = true, aiInsights = true).apply {
        isSoundEnabled.value = false
        isCrtEnabled.value = true
        isHapticsEnabled.value = false
        isReduceMotionEnabled.value = true
        isNotificationsEnabled.value = false
        isNotificationSoundEnabled.value = false
        isNotificationVibrationEnabled.value = false
    }

    @Test
    fun theSettings_comeBackOnTheNewPhone() = runBlocking {
        val json = DataExportImport.exportToJson(restorer(oldPhone, chosen()).snapshot())
        val fresh = FakeSettingsRepository()

        restorer(newPhone, fresh).restore(DataExportImport.importFromJson(json))

        assertEquals(false, fresh.isSoundEnabled.value)
        assertEquals(true, fresh.isCrtEnabled.value)
        assertEquals(false, fresh.isHapticsEnabled.value)
        assertEquals(true, fresh.isReduceMotionEnabled.value)
        assertEquals(false, fresh.isNotificationsEnabled.value)
        assertEquals(false, fresh.isNotificationSoundEnabled.value)
        assertEquals(false, fresh.isNotificationVibrationEnabled.value)
        assertEquals(ThemeMode.Comic, fresh.themeMode.value)
        assertEquals(true, fresh.simpleModeEnabled.value)
        assertFalse("the AI Coach consent is given again, not restored from a file", fresh.aiInsightsEnabled.value)
        assertFalse(JSONObject(json).getJSONObject("settings").keys().asSequence().any { it.contains("ai", ignoreCase = true) })
    }

    @Test
    fun aBackupWithoutSettings_leavesThePhonesSettingsAlone() = runBlocking {
        val phone = chosen()
        restorer(newPhone, phone).restore(
            DataExportImport.importFromJson("""{"userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": []}""")
        )
        assertEquals(ThemeMode.Comic, phone.themeMode.value)
        assertEquals(false, phone.isSoundEnabled.value)
    }

    @Test
    fun onlyWhatTheFileHas_isChanged_andAThemeThisVersionLacksIsSkipped() = runBlocking {
        val phone = FakeSettingsRepository(theme = ThemeMode.Pixel)
        restorer(newPhone, phone).restore(
            DataExportImport.importFromJson(
                """{"formatVersion": 4, "settings": {"haptics": false, "theme": "Holographic"}, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 2, "totalXp": 300}, "tasks": []}"""
            )
        )
        assertEquals(false, phone.isHapticsEnabled.value)
        assertEquals(true, phone.isSoundEnabled.value)
        assertEquals(ThemeMode.Pixel, phone.themeMode.value)
    }
}
