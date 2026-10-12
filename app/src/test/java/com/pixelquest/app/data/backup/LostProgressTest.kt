package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * The database was lost but the settings survived, so the app opens an empty dashboard, not the
 * welcome screen: the automatic backup is offered there instead of waiting unnoticed in Settings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LostProgressTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dir = File(context.filesDir, "lost-progress-test")
    private val store = AutoBackups(dir)
    private lateinit var db: AppDatabase
    private lateinit var lost: LostProgress
    private val oct11 = LocalDate.of(2026, 10, 11)

    private fun backup(name: String, xp: Int) =
        """{"formatVersion": 4, "userProfile": {"id": 1, "username": "$name", "avatarId": "avatar_mage", "level": 3, "totalXp": $xp}, "tasks": []}"""

    @Before
    fun setUp(): Unit = runBlocking {
        dir.deleteRecursively()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        lost = LostProgress(BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context)), store)
        // What a lost database starts over with: the unplayed starter hero.
        db.userProfileDao().insertProfile(UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero"))
        Unit
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    @Test
    fun anEmptyGame_isOfferedTheNewestBackup_andRestoringItBringsItBack() = runBlocking {
        store.save(oct11.minusDays(1), backup("Aria", 900))
        store.save(oct11, backup("Aria", 1200))

        val offer = lost.check()!!
        assertEquals(oct11, offer.day)

        lost.restore(offer)
        assertEquals(1200, db.userProfileDao().getProfile().first()!!.totalXp)
        assertNull("the game has progress again", lost.check())
    }

    @Test
    fun aGameWithProgress_isntOfferedAnything() = runBlocking {
        store.save(oct11, backup("Aria", 1200))
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", totalXp = 50))

        assertNull(lost.check())
    }

    @Test
    fun notNow_isntAskedAgain_untilANewerBackup() = runBlocking {
        store.save(oct11, backup("Aria", 1200))
        lost.decline(lost.check()!!)

        assertNull(lost.check())
        assertEquals("still in Settings", listOf(oct11), store.days())

        store.save(oct11.plusDays(1), backup("Aria", 1300))
        assertEquals(oct11.plusDays(1), lost.check()!!.day)
    }

    @Test
    fun anEmptyBackup_isSkipped_forAnOlderOneWithProgress() = runBlocking {
        // Saved before Step 29, when the daily backup also saved a game not yet played.
        store.save(oct11.minusDays(1), backup("Aria", 900))
        store.save(oct11, backup("PixelHero", 0).replace("\"level\": 3", "\"level\": 1"))

        assertEquals(oct11.minusDays(1), lost.check()!!.day)
    }

    @Test
    fun noBackups_noOffer() = runBlocking {
        assertNull(lost.check())
    }
}
