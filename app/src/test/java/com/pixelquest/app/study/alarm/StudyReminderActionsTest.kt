package com.pixelquest.app.study.alarm

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/** The notification's Mark done, Snooze 1 hour and Open, and what it shows. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyReminderActionsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private var now = Instant.parse("2026-10-22T09:00:00Z")
    private val server = FakeStudyReminderServer(me = "aria", now = now)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repo = StudyReminderRepositoryImpl(server, db.studyReminderDao(), { "aria" }, clock = { now.toEpochMilli() })
    private val alarms = StudyReminderSchedulerTest.FakeAlarms()
    private val scheduler = StudyReminderScheduler(repo, db.studyReminderDao(), alarms, MemoryStudyReminderLedger(), clock = { now.toEpochMilli() })
    private val shown = mutableListOf<ReminderAlert>()
    private val cancelled = mutableListOf<String>()
    private val notifier = object : StudyReminderNotifier {
        override fun show(reminder: StudyReminder, alert: ReminderAlert) {
            shown += alert
        }

        override fun cancel(reminderId: String) {
            cancelled += reminderId
        }
    }
    private val alerts = StudyReminderAlertHandler(repo, db.studyReminderDao(), scheduler, notifier, clock = { now.toEpochMilli() })
    private val actions = StudyReminderActionHandler(repo, db.studyReminderDao(), scheduler, alarms, notifier, clock = { now.toEpochMilli() })

    private val exam = StudyReminderDraft(
        title = "Exam", kind = ReminderKind.EXAM, scope = ReminderScope.TIME,
        dueAt = Instant.parse("2026-10-22T12:00:00Z"), weekStart = null, tz = "UTC", alertOffsets = listOf(180, 60)
    )

    private fun onThePhone(): StudyReminder = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
        val seeded = server.seed("p1", "bram", exam)
        repo.refresh("p1")
        scheduler.rescheduleAll()
        seeded
    }

    @After
    fun tearDown() = db.closeAfterWrites()

    private val occurrence = Instant.parse("2026-10-22T12:00:00Z").toEpochMilli()

    @Test
    fun markDone_stopsTheRestOfThatOccurrence_onThisPhone() = runBlocking {
        val reminder = onThePhone()
        alerts.handle(reminder.id, occurrence, 0)
        assertEquals(1, shown.size)
        assertEquals("the hour-before one is next", setOf("${reminder.id}/1"), alarms.armed.keys)

        actions.handle(StudyReminderAction.MARK_DONE, reminder.id, occurrence)
        assertEquals(listOf(reminder.id), cancelled)
        assertTrue(alarms.armed.isEmpty())
        assertTrue("nothing on the server changes", server.rows.single().title == "Exam")
    }

    @Test
    fun snooze_bringsItBackInAnHour_unlessMarkedDoneMeanwhile() = runBlocking {
        val reminder = onThePhone()
        alerts.handle(reminder.id, occurrence, 0)
        actions.handle(StudyReminderAction.SNOOZE, reminder.id, occurrence)
        val snooze = alarms.armed.getValue(ReminderAlert.snoozeSlot(reminder.id))
        assertEquals(now.plus(Duration.ofHours(1)), snooze.at)

        // A reschedule leaves the snooze alone.
        scheduler.rescheduleAll()
        assertTrue(ReminderAlert.snoozeSlot(reminder.id) in alarms.armed)

        now = snooze.at
        alerts.handle(reminder.id, occurrence, ReminderAlert.SNOOZE_INDEX)
        assertEquals(2, shown.size)

        actions.handle(StudyReminderAction.SNOOZE, reminder.id, occurrence)
        actions.handle(StudyReminderAction.MARK_DONE, reminder.id, occurrence)
        assertTrue("the snooze goes too", ReminderAlert.snoozeSlot(reminder.id) !in alarms.armed)
        alerts.handle(reminder.id, occurrence, ReminderAlert.SNOOZE_INDEX)
        assertEquals(2, shown.size)
    }

    @Test
    fun theNotification_hasItsThreeButtons_andHidesItsTextWhenLocked() = runBlocking {
        val reminder = onThePhone()
        val alert = alarms.armed.values.first()
        AndroidStudyReminderNotifier(context).show(reminder, alert)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(manager).allNotifications.single()
        assertEquals("Exam", posted.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(listOf("Mark done", "Snooze 1 hour", "Open"), posted.actions.map { it.title.toString() })
        assertEquals(Notification.VISIBILITY_PRIVATE, posted.visibility)
        assertEquals("Study reminder", posted.publicVersion.extras.getString(Notification.EXTRA_TITLE))

        AndroidStudyReminderNotifier(context).cancel(reminder.id)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
}
