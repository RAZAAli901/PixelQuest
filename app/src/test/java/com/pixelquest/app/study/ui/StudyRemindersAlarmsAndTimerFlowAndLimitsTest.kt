package com.pixelquest.app.study.ui

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.alarm.StudyDueAlarmScheduler
import com.pixelquest.app.study.data.PrefsStudyTimerStore
import com.pixelquest.app.study.data.RunningTimer
import com.pixelquest.app.study.data.TimerState
import com.pixelquest.app.study.domain.StudyItem
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.testing.FakeAlarmScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/**
 * V046: Study reminders, alarms and timer flow and limits suite.
 *
 * User flow:
 * - Create a study reminder / due item
 * - Day-before alarm is scheduled at (dueAt - 24 hours)
 * - Alarm fires and delivers notification
 * - Run study timer from notification
 *
 * Out-of-bounds cases:
 * - Edit item due date: updates alarm to new trigger time
 * - Delete item: cancels alarm cleanly
 * - Timer duration boundaries: under 5 minutes discarded (null), over 4 hours capped at 240
 * - Process death / app killed while running: timer state and elapsed duration persist across restarts
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyRemindersAlarmsAndTimerFlowAndLimitsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val fakeAlarms = FakeAlarmScheduler()
    private var currentTime = Instant.parse("2026-10-21T10:00:00Z")
    private val account = DaroodAccount { "aria" }
    private lateinit var dueAlarmScheduler: StudyDueAlarmScheduler

    private fun testParty(id: String) = PartyEntity(
        id = id,
        name = "Physics Group",
        leaderId = "aria",
        maxMembers = 10,
        milestone = null,
        milestoneSetAt = null,
        completedAt = null,
        createdAt = currentTime.toEpochMilli(),
        total = 0L,
        entryCount = 0,
        memberCount = 2,
        lastEntryDate = null,
        myRole = PartyRole.LEADER,
        myStatus = MemberStatus.ACTIVE,
        syncedAt = currentTime.toEpochMilli(),
        isPublicRace = false,
        partyType = PartyType.STUDY,
    )

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        db.partyDao().upsertParties(listOf(testParty("party_study_1")))

        dueAlarmScheduler = StudyDueAlarmScheduler(
            context = context,
            alarmScheduler = fakeAlarms,
            dao = db.studyDao(),
            account = account,
            clock = { currentTime.toEpochMilli() },
        )
    }

    @After
    fun tearDown() {
        db.close()
        // Clear shared preferences used for timer tests
        context.getSharedPreferences("study_timer_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun userFlow_createReminder_getDueDayBeforeAlarm_andRunTimerFromNotification() = runBlocking {
        // 1. Create a study item due in 48 hours
        val dueAt = currentTime.plus(Duration.ofHours(48))
        val studyItem = StudyItem(
            id = "quiz_phys_1",
            partyId = "party_study_1",
            kind = StudyItemKind.QUIZ,
            title = "Waves and Optics Quiz",
            subject = "Physics",
            notes = "Chapters 4-6",
            dueAt = dueAt,
            pinned = true,
            createdBy = "aria",
            createdAt = currentTime,
        )

        // Schedule day-before alarm
        val scheduled = dueAlarmScheduler.schedule(studyItem)
        assertTrue("Alarm must be scheduled", scheduled)
        assertEquals(1, fakeAlarms.scheduledAlarms.size)

        // Trigger time must be exactly (dueAt - 24 hours) = currentTime + 24 hours
        val expectedTrigger = dueAt.toEpochMilli() - Duration.ofHours(24).toMillis()
        assertEquals(expectedTrigger, fakeAlarms.scheduledAlarms.first().atMillis)

        // 2. Advance time to trigger moment (dueAt - 24 hours)
        currentTime = Instant.ofEpochMilli(expectedTrigger)

        // 3. User starts timer from notification
        val timerStore = PrefsStudyTimerStore(context)
        assertNull("Timer not running initially", timerStore.running.value)

        val timerStartMillis = currentTime.toEpochMilli()
        timerStore.start(studyItem.partyId, timerStartMillis, "Physics Group")

        val running = timerStore.running.value
        assertNotNull("Timer is running", running)
        assertEquals(TimerState.RUNNING, running?.state)
        assertEquals("party_study_1", running?.partyId)
        assertEquals(0L, running?.elapsedMillisAt(timerStartMillis))

        // Timer counts elapsed time
        assertEquals(60_000L, running?.elapsedMillisAt(timerStartMillis + 60_000L))
    }

    @Test
    fun alarm_afterEditOrDelete_updatesOrCancelsAlarmProperly() = runBlocking {
        val initialDueAt = currentTime.plus(Duration.ofHours(48))
        val item = StudyItem(
            id = "item_edit_test",
            partyId = "party_study_1",
            kind = StudyItemKind.ASSIGNMENT,
            title = "Mechanics Problem Set",
            subject = "Physics",
            notes = null,
            dueAt = initialDueAt,
            pinned = false,
            createdBy = "aria",
            createdAt = currentTime,
        )

        // 1. Initially scheduled for dueAt - 24h
        dueAlarmScheduler.schedule(item)
        assertEquals(1, fakeAlarms.scheduledAlarms.size)
        val initialExpectedTrigger = initialDueAt.toEpochMilli() - Duration.ofHours(24).toMillis()
        assertEquals(initialExpectedTrigger, fakeAlarms.scheduledAlarms.first().atMillis)

        // 2. Edit item: due date extended to 72 hours
        val updatedDueAt = currentTime.plus(Duration.ofHours(72))
        val editedItem = item.copy(dueAt = updatedDueAt)

        // Reschedule on edit
        dueAlarmScheduler.cancel(item.id)
        dueAlarmScheduler.schedule(editedItem)
        assertEquals(1, fakeAlarms.scheduledAlarms.size)
        val newExpectedTrigger = updatedDueAt.toEpochMilli() - Duration.ofHours(24).toMillis()
        assertEquals(newExpectedTrigger, fakeAlarms.scheduledAlarms.first().atMillis)

        // 3. Delete item: alarm cancelled
        dueAlarmScheduler.cancel(editedItem.id)
        assertEquals(2, fakeAlarms.cancelledAlarms.size)
        assertEquals(0, fakeAlarms.scheduledAlarms.size)
    }

    @Test
    fun timerDurationLimits_under5Minutes_and_over4Hours() {
        val t0 = 1_000_000L
        val timer = RunningTimer("party_1", t0, "Study Circle")

        // Under 5 minutes: null (too short to log, discarded)
        assertNull(timer.loggedAt(t0 + 60_000L)) // 1 min
        assertNull(timer.loggedAt(t0 + 4 * 60_000L)) // 4 min
        assertNull(timer.loggedAt(t0 + 4 * 60_000L + 59_000L)) // 4m 59s

        // Exactly 5 minutes: logged as 5
        assertEquals(5, timer.loggedAt(t0 + 5 * 60_000L))

        // Normal session (45 minutes): logged as 45
        assertEquals(45, timer.loggedAt(t0 + 45 * 60_000L))

        // Exactly 4 hours (240 minutes): logged as 240
        assertEquals(240, timer.loggedAt(t0 + 240 * 60_000L))

        // Over 4 hours (e.g. 5 hours / 300 minutes): capped at 240
        assertEquals(240, timer.loggedAt(t0 + 300 * 60_000L))
        assertEquals(240, timer.loggedAt(t0 + 600 * 60_000L)) // 10 hours
    }

    @Test
    fun appKilledWhileRunning_timerPersistsAcrossProcessDeath() {
        val t0 = 5_000_000L
        val storeProcess1 = PrefsStudyTimerStore(context)
        storeProcess1.start("party_chem", t0, "Organic Chemistry")

        // 10 minutes pass, timer paused
        val t1 = t0 + 10 * 60_000L
        storeProcess1.pause(t1)

        // Simulate app kill / process death: create new PrefsStudyTimerStore instance
        val storeProcess2 = PrefsStudyTimerStore(context)
        val restored = storeProcess2.running.value

        assertNotNull("Timer must be restored after app kill", restored)
        assertEquals("party_chem", restored?.partyId)
        assertEquals("Organic Chemistry", restored?.partyName)
        assertEquals(TimerState.PAUSED, restored?.state)
        assertTrue(restored?.isPaused == true)

        // Elapsed time must remain 10 minutes (600,000ms) regardless of later check time
        assertEquals(600_000L, restored?.elapsedMillisAt(t1 + 500_000L))

        // Resume in new process
        val t2 = t1 + 5 * 60_000L
        storeProcess2.resume(t2)

        // Simulate second app kill after 20 minutes of resumed study
        val t3 = t2 + 20 * 60_000L
        val storeProcess3 = PrefsStudyTimerStore(context)
        val restoredAgain = storeProcess3.running.value

        assertNotNull(restoredAgain)
        assertEquals(TimerState.RUNNING, restoredAgain?.state)
        // 10m before pause + 20m after resume = 30m total active study (1,800,000ms)
        assertEquals(1_800_000L, restoredAgain?.elapsedMillisAt(t3))
        assertEquals(30, restoredAgain?.loggedAt(t3))
    }
}
