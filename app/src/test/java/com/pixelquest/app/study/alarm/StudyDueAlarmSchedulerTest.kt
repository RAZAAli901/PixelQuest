package com.pixelquest.app.study.alarm

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.scheduling.AlarmScheduler
import com.pixelquest.app.study.data.local.StudyCompletionEntity
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyDueAlarmSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val fakeAlarms = FakeAlarmScheduler()
    private var now = Instant.parse("2026-10-21T10:00:00Z")
    private val account = DaroodAccount { "aria" }
    private lateinit var scheduler: StudyDueAlarmScheduler

    private fun party(id: String) = PartyEntity(
        id, "Physics group", "aria", 10, null, null, null, 0, 0, 0, 2, null, PartyRole.LEADER, MemberStatus.ACTIVE, 0, partyType = PartyType.STUDY,
    )

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        db.partyDao().upsertParties(listOf(party("party_1")))
        scheduler = StudyDueAlarmScheduler(
            context = context,
            alarmScheduler = fakeAlarms,
            dao = db.studyDao(),
            account = account,
            clock = { now.toEpochMilli() },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun schedulesAlarmAtDueMinus24Hours() = runBlocking {
        // Due 48 hours from now
        val dueAt = now.plus(Duration.ofHours(48))
        val item = StudyItem(
            id = "item_1",
            partyId = "party_1",
            kind = StudyItemKind.ASSIGNMENT,
            title = "Physics Problem Set",
            subject = null,
            notes = null,
            dueAt = dueAt,
            pinned = false,
            createdBy = "aria",
            createdAt = now,
        )

        val scheduled = scheduler.schedule(item)
        assertTrue(scheduled)
        assertEquals(1, fakeAlarms.scheduled.size)

        // Alarm trigger moment must be exactly 24 hours before dueAt (i.e. now + 24 hours)
        val expectedTrigger = dueAt.toEpochMilli() - Duration.ofHours(24).toMillis()
        assertEquals(expectedTrigger, fakeAlarms.scheduled.first().first)
    }

    @Test
    fun retroactiveUnder24Hours_isNotScheduled() = runBlocking {
        // Due in only 12 hours (< 24 hours remain)
        val dueAt = now.plus(Duration.ofHours(12))
        val item = StudyItem(
            id = "item_2",
            partyId = "party_1",
            kind = StudyItemKind.QUIZ,
            title = "Quick Quiz",
            subject = null,
            notes = null,
            dueAt = dueAt,
            pinned = false,
            createdBy = "aria",
            createdAt = now,
        )

        val scheduled = scheduler.schedule(item)
        assertFalse(scheduled)
        assertTrue(fakeAlarms.scheduled.isEmpty())
    }

    @Test
    fun itemWithoutDueDate_isNotScheduled() = runBlocking {
        val item = StudyItem(
            id = "item_3",
            partyId = "party_1",
            kind = StudyItemKind.ASSIGNMENT,
            title = "Reading",
            subject = null,
            notes = null,
            dueAt = null,
            pinned = false,
            createdBy = "aria",
            createdAt = now,
        )

        val scheduled = scheduler.schedule(item)
        assertFalse(scheduled)
        assertTrue(fakeAlarms.scheduled.isEmpty())
    }

    @Test
    fun completedItem_isNotScheduled() = runBlocking {
        val dueAt = now.plus(Duration.ofHours(48))
        val item = StudyItem(
            id = "item_4",
            partyId = "party_1",
            kind = StudyItemKind.ASSIGNMENT,
            title = "Already Done",
            subject = null,
            notes = null,
            dueAt = dueAt,
            pinned = false,
            createdBy = "aria",
            createdAt = now,
        )

        // Mark completed in DB
        db.studyDao().upsertCompletions(
            listOf(
                StudyCompletionEntity(
                    id = 1L,
                    partyId = "party_1",
                    itemId = "item_4",
                    userId = "aria",
                    completedAt = now.toEpochMilli(),
                    localDate = LocalDate.parse("2026-10-21"),
                    tz = "UTC",
                ),
            ),
        )

        val scheduled = scheduler.schedule(item)
        assertFalse(scheduled)
        assertTrue(fakeAlarms.scheduled.isEmpty())
    }

    @Test
    fun uniqueRequestCodePerItem() {
        val code1 = scheduler.requestCodeForItem("item_1")
        val code2 = scheduler.requestCodeForItem("item_2")
        val code3 = scheduler.requestCodeForItem("assignment_42")

        assertNotEquals(code1, code2)
        assertNotEquals(code2, code3)
        assertTrue(code1 >= StudyDueAlarmScheduler.REQUEST_CODE_BASE)
        assertTrue(code2 >= StudyDueAlarmScheduler.REQUEST_CODE_BASE)
        assertTrue(code3 >= StudyDueAlarmScheduler.REQUEST_CODE_BASE)
    }

    @Test
    fun cancel_cancelsAlarm() = runBlocking {
        val dueAt = now.plus(Duration.ofHours(48))
        val item = StudyItem(
            id = "item_cancel",
            partyId = "party_1",
            kind = StudyItemKind.QUIZ,
            title = "Quiz To Cancel",
            subject = null,
            notes = null,
            dueAt = dueAt,
            pinned = false,
            createdBy = "aria",
            createdAt = now,
        )
        scheduler.schedule(item)
        assertEquals(1, fakeAlarms.scheduled.size)

        scheduler.cancel("item_cancel")
        assertEquals(1, fakeAlarms.cancelled.size)
    }

    @Test
    fun rescheduleAll_schedulesOnlyEligibleUnfinishedItems() = runBlocking {
        // Item 1: eligible (due in 30h)
        db.studyDao().upsertItems(
            listOf(
                StudyItemEntity(
                    id = "item_a",
                    partyId = "party_1",
                    kind = StudyItemKind.ASSIGNMENT,
                    title = "Eligible",
                    subject = null,
                    notes = null,
                    dueAt = now.plus(Duration.ofHours(30)).toEpochMilli(),
                    pinned = false,
                    createdBy = "aria",
                    createdAt = now.toEpochMilli(),
                ),
                // Item 2: ineligible (due in 10h)
                StudyItemEntity(
                    id = "item_b",
                    partyId = "party_1",
                    kind = StudyItemKind.QUIZ,
                    title = "Too Soon",
                    subject = null,
                    notes = null,
                    dueAt = now.plus(Duration.ofHours(10)).toEpochMilli(),
                    pinned = false,
                    createdBy = "aria",
                    createdAt = now.toEpochMilli(),
                ),
                // Item 3: no due date
                StudyItemEntity(
                    id = "item_c",
                    partyId = "party_1",
                    kind = StudyItemKind.ASSIGNMENT,
                    title = "No Due Date",
                    subject = null,
                    notes = null,
                    dueAt = null,
                    pinned = false,
                    createdBy = "aria",
                    createdAt = now.toEpochMilli(),
                ),
            ),
        )

        val scheduled = scheduler.rescheduleAll()
        assertEquals(listOf("item_a"), scheduled)
        assertEquals(1, fakeAlarms.scheduled.size)
    }

    @Test
    fun cancelPartyAlarms_cancelsAllItemsInParty() = runBlocking {
        db.partyDao().upsertParties(listOf(party("party_2")))
        db.studyDao().upsertItems(
            listOf(
                StudyItemEntity(
                    id = "item_p1_1",
                    partyId = "party_1",
                    kind = StudyItemKind.ASSIGNMENT,
                    title = "HW 1",
                    subject = null,
                    notes = null,
                    dueAt = now.plus(Duration.ofHours(30)).toEpochMilli(),
                    pinned = false,
                    createdBy = "aria",
                    createdAt = now.toEpochMilli(),
                ),
                StudyItemEntity(
                    id = "item_p1_2",
                    partyId = "party_1",
                    kind = StudyItemKind.QUIZ,
                    title = "Quiz 1",
                    subject = null,
                    notes = null,
                    dueAt = now.plus(Duration.ofHours(40)).toEpochMilli(),
                    pinned = false,
                    createdBy = "aria",
                    createdAt = now.toEpochMilli(),
                ),
                StudyItemEntity(
                    id = "item_p2_1",
                    partyId = "party_2",
                    kind = StudyItemKind.QUIZ,
                    title = "Quiz 2",
                    subject = null,
                    notes = null,
                    dueAt = now.plus(Duration.ofHours(50)).toEpochMilli(),
                    pinned = false,
                    createdBy = "aria",
                    createdAt = now.toEpochMilli(),
                ),
            ),
        )

        // Schedule them
        scheduler.schedule(db.studyDao().getItem("item_p1_1")!!)
        scheduler.schedule(db.studyDao().getItem("item_p1_2")!!)
        scheduler.schedule(db.studyDao().getItem("item_p2_1")!!)
        assertEquals(3, fakeAlarms.scheduled.size)

        // Cancel party_1
        scheduler.cancelPartyAlarms("party_1")
        assertEquals(2, fakeAlarms.cancelled.size)
    }

    class FakeAlarmScheduler : AlarmScheduler {
        val scheduled = mutableListOf<Pair<Long, PendingIntent>>()
        val cancelled = mutableListOf<PendingIntent>()

        override fun canScheduleExactAlarms(): Boolean = true

        override fun schedule(
            atMillis: Long,
            pendingIntent: PendingIntent,
            isAlarmClock: Boolean,
            clockInfoIntent: PendingIntent?,
        ): Boolean {
            scheduled += atMillis to pendingIntent
            return true
        }

        override fun cancel(pendingIntent: PendingIntent) {
            cancelled += pendingIntent
        }

        override fun openExactAlarmSettingsIntent(): Intent = Intent("ACTION_SETTINGS")
    }
}
