package com.pixelquest.app.study.alarm

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.ReminderSave
import com.pixelquest.app.study.data.StudyReminderRepositoryImpl
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.testing.FakeStudyReminderServer
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Alarms follow the phone's reminders: made, changed, deleted, synced, or waiting to send. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyReminderAlarmSyncTest {

    private val now = Instant.parse("2026-10-21T10:00:00Z")
    private val server = FakeStudyReminderServer(me = "aria", now = now)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = StudyReminderRepositoryImpl(server, db.studyReminderDao(), { "aria" }, clock = { now.toEpochMilli() })
    private val alarms = StudyReminderSchedulerTest.FakeAlarms()
    private val scheduler = StudyReminderScheduler(repo, db.studyReminderDao(), alarms, MemoryStudyReminderLedger(), clock = { now.toEpochMilli() })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val exam = StudyReminderDraft(
        title = "Exam", kind = ReminderKind.EXAM, scope = ReminderScope.TIME,
        dueAt = Instant.parse("2026-10-23T10:00:00Z"), weekStart = null, tz = "UTC", alertOffsets = listOf(60)
    )

    @After
    fun tearDown() {
        scope.cancel()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(300) {
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun armedAt() = synchronized(alarms) { alarms.armed.values.map { it.at }.sorted() }

    @Test
    fun everyChange_reArmsTheAlarms() = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
        StudyReminderAlarmSync(db.studyReminderDao(), scheduler).start(scope)

        // Someone else's, brought by a sync.
        val theirs = server.seed("p1", "bram", exam)
        repo.refresh("p1")
        waitFor("armed from the sync") { armedAt() == listOf(Instant.parse("2026-10-23T09:00:00Z")) }

        // Made here offline: it alerts before it's sent.
        server.online = false
        assertEquals(ReminderSave.Waiting, repo.create("p1", exam.copy(title = "Mine", audience = ReminderAudience.ME, dueAt = Instant.parse("2026-10-22T10:00:00Z"))))
        waitFor("armed while waiting") { armedAt().size == 2 }

        // Changed by its creator, and deleted: followed both times.
        server.online = true
        repo.sendPending()
        server.rows.replaceAll { if (it.id == theirs.id) it.copy(alertOffsets = listOf(120)) else it }
        repo.refresh("p1")
        waitFor("moved") { armedAt() == listOf(Instant.parse("2026-10-22T09:00:00Z"), Instant.parse("2026-10-23T08:00:00Z")) }
        server.rows.removeIf { it.id == theirs.id }
        repo.refresh("p1")
        waitFor("cancelled") { armedAt() == listOf(Instant.parse("2026-10-22T09:00:00Z")) }
    }

    @Test
    fun aRefusedNewOne_neverAlerts() = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
        // 100 of hers there already: the server refuses another.
        repeat(100) { server.seed("p1", "aria", exam.copy(title = "Filler $it")) }
        assertTrue(repo.create("p1", exam.copy(title = "One more")) is ReminderSave.Refused)
        assertTrue(repo.allReminders().none { it.title == "One more" })
    }
}
