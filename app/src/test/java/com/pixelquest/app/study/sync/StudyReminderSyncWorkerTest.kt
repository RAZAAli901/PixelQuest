package com.pixelquest.app.study.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.ReminderSave
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.testing.FakeStudyReminderServer
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Study reminders reach the phone in the background: every 30 minutes with a connection, and what waits goes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyReminderSyncWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val server = FakeStudyReminderServer(me = "aria")
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private var sendsAsked = 0
    private val repo = StudyReminderRepositoryImpl(server, db.studyReminderDao(), { "aria" }, clock = { server.now.toEpochMilli() }) { sendsAsked++ }

    private val quiz = StudyReminderDraft(
        title = "Quiz", kind = ReminderKind.QUIZ, scope = ReminderScope.TIME,
        dueAt = Instant.parse("2026-10-23T10:00:00Z"), weekStart = null, tz = "UTC"
    )

    @After
    fun tearDown() = db.closeAfterWrites()

    private fun party() = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
    }

    private fun run(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<StudyReminderSyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StudyReminderSyncWorker(appContext, workerParameters, repo)
            })
            .build()
            .doWork()
    }

    @Test
    fun aRun_sendsWhatWaits_thenBringsEveryonesReminders() = runBlocking {
        party()
        server.online = false
        assertEquals(ReminderSave.Waiting, repo.create("p1", quiz.copy(title = "Mine")))
        assertEquals("asked to go once there's a connection", 1, sendsAsked)
        assertEquals("tried again later", ListenableWorker.Result.retry(), run())

        server.online = true
        server.seed("p1", "bram", quiz.copy(title = "Bram's"))
        assertEquals(ListenableWorker.Result.success(), run())
        assertEquals(setOf("Mine", "Bram's"), repo.allReminders().map { it.title }.toSet())
        assertTrue(db.studyReminderDao().pendingToSend().isEmpty())
    }

    @Test
    fun itsSchedule_isEvery30Minutes_withAConnection() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        StudyReminderSyncWorker.schedule(context)
        StudyReminderSyncWorker.schedule(context)
        val work = WorkManager.getInstance(context).getWorkInfosForUniqueWork("pixelquest_study_reminders_sync").get()
        assertEquals("one, kept", 1, work.size)
        assertEquals(WorkInfo.State.ENQUEUED, work.single().state)
        assertEquals(TimeUnit.MINUTES.toMillis(30), work.single().periodicityInfo!!.repeatIntervalMillis)
        assertEquals(androidx.work.NetworkType.CONNECTED, work.single().constraints.requiredNetworkType)
    }
}
