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
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderRepeat
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * What the phone arms for each kind of reminder (docs/study-reminders.md, When it alerts): a week,
 * a day and a time; repeating; changed and deleted; and across time zones and changes of clock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyReminderSchedulingScenariosTest {

    private val karachi = ZoneId.of("Asia/Karachi")
    private fun pk(text: String): Instant = LocalDateTime.parse(text).atZone(karachi).toInstant()

    // Wednesday 21 October 2026, 15:00 in Karachi.
    private var now = pk("2026-10-21T15:00")
    private val server = FakeStudyReminderServer(me = "aria", now = now)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = StudyReminderRepositoryImpl(server, db.studyReminderDao(), { "aria" }, clock = { now.toEpochMilli() })
    private val alarms = StudyReminderSchedulerTest.FakeAlarms()
    private val scheduler = StudyReminderScheduler(repo, db.studyReminderDao(), alarms, MemoryStudyReminderLedger(), clock = { now.toEpochMilli() })
    private val phoneZone: TimeZone = TimeZone.getDefault()

    private fun draft(scope: ReminderScope, dueAt: Instant? = null, weekStart: LocalDate? = null, repeat: ReminderRepeat = ReminderRepeat.NONE, tz: String = "Asia/Karachi") =
        StudyReminderDraft(title = "Quiz", kind = ReminderKind.QUIZ, scope = scope, dueAt = dueAt, weekStart = weekStart, tz = tz, repeat = repeat)

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

    private fun armed(): List<Instant> = runBlocking { scheduler.rescheduleAll() }.map { it.at }.sorted()

    @After
    fun tearDown() {
        TimeZone.setDefault(phoneZone)
        db.closeAfterWrites()
    }

    @Test
    fun aWeek_nextWeeks_MondayMorning_andSaturdayEvening() {
        onThePhone(draft(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 10, 26)))
        assertEquals(listOf(pk("2026-10-26T09:00"), pk("2026-10-31T18:00")), armed())
    }

    @Test
    fun thisWeeks_week_onlySaturdayLeft() {
        onThePhone(draft(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 10, 19)))
        assertEquals(listOf(pk("2026-10-24T18:00")), armed())
    }

    @Test
    fun aDay_theEveningBefore_andThatMorning() {
        onThePhone(draft(ReminderScope.DAY, dueAt = pk("2026-10-23T00:00")))
        assertEquals(listOf(pk("2026-10-22T18:00"), pk("2026-10-23T07:00")), armed())
    }

    @Test
    fun aTime_aDayAndHalfAnHourBefore() {
        onThePhone(draft(ReminderScope.TIME, dueAt = pk("2026-10-23T10:00")))
        assertEquals(listOf(pk("2026-10-22T10:00"), pk("2026-10-23T09:30")), armed())
    }

    @Test
    fun aWeeklyRepeat_movesOnToNextWeek_onceThisOnesAlertsHavePassed() {
        onThePhone(draft(ReminderScope.TIME, dueAt = pk("2026-10-07T10:00"), repeat = ReminderRepeat.WEEKLY))
        assertEquals(listOf(pk("2026-10-27T10:00"), pk("2026-10-28T09:30")), armed())
        now = pk("2026-10-28T09:31")
        assertEquals(listOf(pk("2026-11-03T10:00"), pk("2026-11-04T09:30")), armed())
    }

    @Test
    fun aDailyRepeat_alwaysHasItsNext() {
        onThePhone(draft(ReminderScope.DAY, dueAt = pk("2026-10-01T00:00"), repeat = ReminderRepeat.DAILY))
        // 15:00: today's morning alert passed, tomorrow's evening-before one is tonight.
        assertEquals(listOf(pk("2026-10-21T18:00"), pk("2026-10-22T07:00")), armed())
    }

    @Test
    fun anEdit_movesItsAlarms_andADelete_cancelsThem() = runBlocking {
        val (reminder) = onThePhone(draft(ReminderScope.TIME, dueAt = pk("2026-10-23T10:00")))
        armed()
        server.rows.replaceAll { if (it.id == reminder.id) it.copy(dueAt = pk("2026-10-24T12:00")) else it }
        repo.refresh("p1")
        assertEquals(listOf(pk("2026-10-23T12:00"), pk("2026-10-24T11:30")), armed())
        assertEquals(setOf("${reminder.id}/0", "${reminder.id}/1"), alarms.armed.keys)

        server.rows.clear()
        repo.refresh("p1")
        assertTrue(armed().isEmpty())
        assertTrue("its alarms cancelled", alarms.armed.isEmpty())
    }

    @Test
    fun aChangeOfThePhonesZone_keepsTheReminderAtItsOwnTimes() {
        onThePhone(draft(ReminderScope.TIME, dueAt = pk("2026-10-23T10:00")))
        val before = armed()
        // Flying to London: the exam is still at 10:00 in Karachi, 06:00 in London.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
        assertEquals(before, armed())
    }

    @Test
    fun aReminderInAnotherZone_alertsAtItsLocalTimes_acrossItsClockChange() {
        // Berlin goes to winter time on Sunday 25 October: Monday 09:00 is 08:00 UTC, Saturday 18:00 summer time is 16:00 UTC.
        onThePhone(draft(ReminderScope.WEEK, weekStart = LocalDate.of(2026, 10, 19), repeat = ReminderRepeat.WEEKLY, tz = "Europe/Berlin"))
        now = Instant.parse("2026-10-24T16:30:00Z")
        assertEquals(listOf(Instant.parse("2026-10-26T08:00:00Z"), Instant.parse("2026-10-31T17:00:00Z")), armed())
    }
}
