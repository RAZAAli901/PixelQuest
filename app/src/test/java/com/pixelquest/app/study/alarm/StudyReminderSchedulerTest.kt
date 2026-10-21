package com.pixelquest.app.study.alarm

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
import com.pixelquest.app.study.data.local.StudyReminderDoneEntity
import com.pixelquest.app.study.domain.ReminderAlert
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
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
import java.time.Duration
import java.time.Instant

/** Study reminders turned into alarms: armed afresh each time, never twice, and gone with the reminder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyReminderSchedulerTest {

    private var now = Instant.parse("2026-10-21T10:00:00Z")
    private val server = FakeStudyReminderServer(me = "aria", now = now)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = StudyReminderRepositoryImpl(server, db.studyReminderDao(), { "aria" }, clock = { now.toEpochMilli() })
    private val alarms = FakeAlarms()
    private val ledger = MemoryStudyReminderLedger()
    private val scheduler = StudyReminderScheduler(repo, db.studyReminderDao(), alarms, ledger, clock = { now.toEpochMilli() })
    private val shown = mutableListOf<ReminderAlert>()
    private val handler = StudyReminderAlertHandler(
        repo, db.studyReminderDao(), scheduler,
        object : StudyReminderNotifier {
            override fun show(reminder: StudyReminder, alert: ReminderAlert) {
                shown += alert
            }
        },
        clock = { now.toEpochMilli() }
    )

    class FakeAlarms : StudyReminderAlarms {
        val armed = mutableMapOf<String, ReminderAlert>()
        override fun schedule(alert: ReminderAlert) {
            armed[alert.slot] = alert
        }

        override fun cancel(slot: String) {
            armed.remove(slot)
        }
    }

    private val exam = StudyReminderDraft(
        title = "Exam", kind = ReminderKind.EXAM, scope = ReminderScope.TIME,
        dueAt = Instant.parse("2026-10-22T10:00:00Z"), weekStart = null, tz = "UTC", alertOffsets = listOf(1440, 30)
    )

    private fun onThePhone(vararg drafts: StudyReminderDraft): List<StudyReminder> = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
        val seeded = drafts.map { server.seed("p1", "bram", it) }
        repo.refresh("p1")
        seeded
    }

    @After
    fun tearDown() = db.closeAfterWrites()

    @Test
    fun eachAlertStillToCome_isArmed_andGoesWithItsReminder() = runBlocking {
        val (reminder) = onThePhone(exam)
        scheduler.rescheduleAll()
        assertEquals(listOf(Instant.parse("2026-10-22T09:30:00Z")), alarms.armed.values.map { it.at })
        assertEquals("the day before passed at 10:00 today, as now", setOf("${reminder.id}/1"), ledger.armed())

        server.rows.clear()
        repo.refresh("p1")
        scheduler.rescheduleAll()
        assertTrue("cancelled with its reminder", alarms.armed.isEmpty())
        assertTrue(ledger.armed().isEmpty())
    }

    @Test
    fun anAlert_showsOnce_thenTheNextIsArmed() = runBlocking {
        val (reminder) = onThePhone(exam.copy(alertOffsets = listOf(60, 30)))
        scheduler.rescheduleAll()
        val first = alarms.armed.getValue("${reminder.id}/0")

        now = first.at
        handler.handle(reminder.id, first.occurrence.start.toInstant().toEpochMilli(), first.index)
        assertEquals(listOf(first.at), shown.map { it.at })
        assertEquals("only the 30 minutes' left", setOf("${reminder.id}/1"), alarms.armed.keys)

        // The same alarm again (a reschedule raced it): never a second time.
        handler.handle(reminder.id, first.occurrence.start.toInstant().toEpochMilli(), first.index)
        assertEquals(1, shown.size)
    }

    @Test
    fun aDoneOccurrence_alertsNoMore() = runBlocking {
        val (reminder) = onThePhone(exam.copy(alertOffsets = listOf(60, 30)))
        scheduler.rescheduleAll()
        val occurrence = alarms.armed.values.first().occurrence.start.toInstant().toEpochMilli()
        db.studyReminderDao().markDone(StudyReminderDoneEntity(reminder.id, occurrence, now.toEpochMilli()))

        scheduler.rescheduleAll()
        assertTrue(alarms.armed.isEmpty())
        now = Instant.parse("2026-10-22T09:00:00Z")
        handler.handle(reminder.id, occurrence, 0)
        assertTrue("its alarm, if it still came, shows nothing", shown.isEmpty())
    }

    @Test
    fun anAlertMissed_whileThePhoneWasOff_comesAtARestart_withinTheHour() = runBlocking {
        // A day and 20 minutes before 10:00 tomorrow is 20 minutes ago; 22 and a half hours before, 11:30 today.
        onThePhone(exam.copy(alertOffsets = listOf(1440 + 20, 1440 - 90)))
        scheduler.rescheduleAll()
        assertEquals("not late on an ordinary arming", listOf(Instant.parse("2026-10-21T11:30:00Z")), alarms.armed.values.map { it.at })
        scheduler.rescheduleAll(catchUp = true)
        assertEquals(
            listOf(Instant.parse("2026-10-21T09:40:00Z"), Instant.parse("2026-10-21T11:30:00Z")),
            alarms.armed.values.map { it.at }.sorted()
        )
    }

    @Test
    fun aStaleAlarm_orOneForAMovedReminder_showsNothing() = runBlocking {
        val (reminder) = onThePhone(exam.copy(alertOffsets = listOf(30)))
        scheduler.rescheduleAll()
        val alert = alarms.armed.values.single()
        now = alert.at.plus(Duration.ofHours(2))
        handler.handle(reminder.id, alert.occurrence.start.toInstant().toEpochMilli(), alert.index)
        assertTrue("two hours late is too late", shown.isEmpty())

        now = alert.at
        handler.handle(reminder.id, alert.occurrence.start.toInstant().toEpochMilli(), 3)
        assertTrue("no fourth alert", shown.isEmpty())
        handler.handle("gone", alert.occurrence.start.toInstant().toEpochMilli(), 0)
        assertTrue(shown.isEmpty())
    }
}
