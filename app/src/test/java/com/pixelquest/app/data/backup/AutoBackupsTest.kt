package com.pixelquest.app.data.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.worker.AutoBackupWorker
import kotlinx.coroutines.runBlocking
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

/**
 * A daily backup is kept on the phone, the last three days, so a corrupt database (which Android
 * deletes and starts over) or a mistake doesn't cost everything when no backup was exported.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AutoBackupsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dir = File(context.filesDir, "auto-backups-test")
    private val store = AutoBackups(dir)
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        dir.deleteRecursively()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    @Test
    fun theLastThreeDays_areKept_newestFirst() {
        val oct = (8..12).map { LocalDate.of(2026, 10, it) }
        oct.forEach { store.save(it, """{"day": "$it"}""") }

        assertEquals(listOf(12, 11, 10).map { LocalDate.of(2026, 10, it) }, store.days())
        assertEquals("""{"day": "2026-10-12"}""", store.read(LocalDate.of(2026, 10, 12)))
        assertEquals(null, store.read(LocalDate.of(2026, 10, 8)))
    }

    @Test
    fun aSecondBackupOnTheSameDay_replacesTheFirst() {
        val day = LocalDate.of(2026, 10, 12)
        store.save(day, "first")
        store.save(day, "second")

        assertEquals(listOf(day), store.days())
        assertEquals("second", store.read(day))
    }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        TestListenableWorkerBuilder<AutoBackupWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    AutoBackupWorker(appContext, workerParameters, restorer, store)
            })
            .build()
            .doWork()
    }

    @Test
    fun theDailyWorker_savesAPlayedGame() = runBlocking {
        db.userProfileDao().insertProfile(UserProfileEntity(username = "Aria", avatarId = "avatar_mage", level = 2, totalXp = 450))

        assertEquals(ListenableWorker.Result.success(), runWorker())

        assertEquals(listOf(LocalDate.now()), store.days())
        val saved = DataExportImport.importFromJson(store.read(LocalDate.now())!!)
        assertEquals("Aria", saved.userProfile!!.username)
        assertEquals(450, saved.userProfile!!.totalXp)
    }

    @Test
    fun aGameNotYetPlayed_isntSaved() = runBlocking {
        db.userProfileDao().insertProfile(UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero"))

        runWorker()

        assertTrue(store.days().isEmpty())
    }

    @Test
    fun aFreshInstallsMissedStarterQuests_dontCount() = runBlocking {
        // What a fresh install looks like in the evening: starter quests whose time has passed, logged as missed.
        db.userProfileDao().insertProfile(UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero"))
        db.taskCompletionLogDao().insertLog(
            com.pixelquest.app.data.local.entity.TaskCompletionLogEntity(taskId = 1, completedDate = LocalDate.now(), wasCompleted = false, pointsAwarded = 0)
        )

        runWorker()

        assertTrue("used to save the empty game", store.days().isEmpty())
    }

    @Test
    fun openingTheApp_queuesOneBackupAfterPlay_andOpeningAgainDoesntPushItBack() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            androidx.work.Configuration.Builder().setExecutor(androidx.work.testing.SynchronousExecutor()).build()
        )
        AutoBackupWorker.scheduleAfterPlay(context)
        val first = androidx.work.WorkManager.getInstance(context).getWorkInfosForUniqueWork(AutoBackupWorker.AFTER_PLAY_WORK_NAME).get().single()

        AutoBackupWorker.scheduleAfterPlay(context)

        val waiting = androidx.work.WorkManager.getInstance(context).getWorkInfosForUniqueWork(AutoBackupWorker.AFTER_PLAY_WORK_NAME).get()
        assertEquals(listOf(first.id), waiting.map { it.id })
        assertEquals(androidx.work.WorkInfo.State.ENQUEUED, waiting.single().state)
    }
}
