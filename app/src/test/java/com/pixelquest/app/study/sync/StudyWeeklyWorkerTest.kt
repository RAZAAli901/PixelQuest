package com.pixelquest.app.study.sync

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.MemoryStudyReminderSettings
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** The Sunday summary: only if turned on, a line per study party with the player's points and place. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyWeeklyWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "aria").apply { now = Instant.parse("2026-10-25T14:00:00Z") }
    private val study = FakeStudyServer(server)
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { ZoneId.of("UTC") })
    private val repo = StudyRepositoryImpl(study, db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { ZoneId.of("UTC") })

    @Before
    fun setUp() {
        NotificationChannels.createAll(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun run(on: Boolean) = runBlocking {
        TestListenableWorkerBuilder<StudyWeeklyWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StudyWeeklyWorker(appContext, workerParameters, parties, repo, MemoryStudyReminderSettings(weekly = on))
            })
            .build()
            .doWork()
    }

    @Test
    fun turnedOn_aLinePerParty_withThePlayersPointsAndPlace() {
        val physics = server.seedParty("Physics group", "aria", "bram", "cleo", type = PartyType.STUDY)
        server.seedParty("Friday circle", "aria")
        val item = study.seedItem(physics, "bram", "Lab report")
        study.seedCompletion(item.id, "bram")
        study.seedSession(physics, "aria", 40)

        run(on = true)
        val sent = notifications.single()
        assertEquals(NotificationChannels.Spec.STUDY.id, sent.channelId)
        assertEquals("Your study week", sent.extras.getString("android.title"))
        assertEquals("Physics group: 4 points, place 2 of 3", sent.extras.getCharSequence("android.text").toString())
    }

    @Test
    fun turnedOff_nothing() {
        server.seedParty("Physics group", "aria", type = PartyType.STUDY)
        run(on = false)
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun itRuns_onSundayEvening() {
        assertEquals(5 * 60L, StudyWeeklyWorker.delayUntilNextRun(LocalDateTime.of(2026, 10, 25, 14, 0)))
        assertEquals(7 * 24 * 60L - 60, StudyWeeklyWorker.delayUntilNextRun(LocalDateTime.of(2026, 10, 25, 20, 0)))
    }
}
