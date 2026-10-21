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
import com.pixelquest.app.study.domain.StudyItemKind
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Reminders of what's due: a day before and two hours before, once each, only for what's unfinished. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyDueWorkerTest {

    private val now = Instant.parse("2026-10-21T10:00:00Z")

    @Test
    fun theCloserReminder_winsAndEachIsSentOnce() {
        val inThirty = DueItem("a", "Physics", "Lab report", now.plus(Duration.ofHours(30)))
        val inTwenty = DueItem("b", "Physics", "Essay", now.plus(Duration.ofHours(20)))
        val inOne = DueItem("c", "Physics", "Waves quiz", now.plus(Duration.ofHours(1)))
        val past = DueItem("d", "Physics", "Old", now.minus(Duration.ofHours(1)))
        assertEquals(
            listOf(inOne to DueReminder.SOON, inTwenty to DueReminder.DAY),
            remindersDue(listOf(inThirty, inTwenty, inOne, past), emptySet(), now)
        )
        assertTrue(remindersDue(listOf(inTwenty, inOne), setOf("b:DAY", "c:SOON"), now).isEmpty())
        // The day one sent already, the two hours one is still to come.
        assertEquals(listOf(inOne to DueReminder.SOON), remindersDue(listOf(inOne), setOf("c:DAY"), now))
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "aria").apply { this.now = this@StudyDueWorkerTest.now }
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

    private fun run(on: Boolean = true) = runBlocking {
        TestListenableWorkerBuilder<StudyDueWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    StudyDueWorker(appContext, workerParameters, parties, repo, db.studyDao(), { server.me }, MemoryStudyReminderSettings(on))
            })
            .build()
            .doWork()
    }

    @Test
    fun itReminds_ofWhatsUnfinished_onTheStudyChannel_once() {
        val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
        study.seedItem(party, "bram", "Waves quiz", StudyItemKind.QUIZ, dueAt = now.plus(Duration.ofHours(1)))
        val done = study.seedItem(party, "bram", "Lab report", dueAt = now.plus(Duration.ofHours(5)))
        study.seedCompletion(done.id, "aria")
        study.seedItem(party, "bram", "Essay", dueAt = now.plus(Duration.ofDays(3)))

        run()
        val sent = notifications.filter { it.channelId == NotificationChannels.Spec.STUDY.id }
        assertEquals(listOf("Due in under 2 hours: Waves quiz"), sent.map { it.extras.getString("android.title") })
        assertEquals("In Physics group. Mark it done once you've finished.", sent.single().extras.getCharSequence("android.text").toString())

        run()
        assertEquals("not again", 1, notifications.size)
    }

    @Test
    fun turnedOff_nothing() {
        val party = server.seedParty("Physics group", "aria", type = PartyType.STUDY)
        study.seedItem(party, "aria", "Waves quiz", StudyItemKind.QUIZ, dueAt = now.plus(Duration.ofHours(1)))
        run(on = false)
        assertTrue(notifications.isEmpty())
    }
}
