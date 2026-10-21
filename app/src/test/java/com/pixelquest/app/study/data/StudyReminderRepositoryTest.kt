package com.pixelquest.app.study.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderProblem
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.StudyReminderDraft
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.testing.FakeStudyReminderServer
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.flow.first
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

/** Study reminders on the phone (docs/study-reminders.md): kept offline, sent once, refusals kept. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyReminderRepositoryTest {

    private val server = FakeStudyReminderServer(me = "aria")
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = StudyReminderRepositoryImpl(server, db.studyReminderDao(), { server.me }, clock = { server.now.toEpochMilli() })

    private val quiz = StudyReminderDraft(
        title = "Quiz this week", kind = ReminderKind.QUIZ, scope = ReminderScope.WEEK,
        dueAt = null, weekStart = LocalDate.of(2026, 10, 19), tz = "Asia/Karachi"
    )
    private val exam = StudyReminderDraft(
        title = "Exam at 10", kind = ReminderKind.EXAM, scope = ReminderScope.TIME,
        dueAt = Instant.parse("2026-10-23T05:00:00Z"), weekStart = null, tz = "Asia/Karachi"
    )

    private fun party(id: String, type: PartyType = PartyType.STUDY, status: MemberStatus = MemberStatus.ACTIVE) = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = id, name = id, leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = status, syncedAt = 0, partyType = type
            )
        ))
        server.leaders[id] = "bram"
    }

    @After
    fun tearDown() = db.closeAfterWrites()

    @Test
    fun aReminderMadeOffline_showsAtOnce_thenGoesOnce() = runBlocking {
        party("p1")
        server.online = false
        assertEquals(ReminderSave.Waiting, repo.create("p1", quiz))
        val waiting = repo.observeReminders("p1").first().single()
        assertEquals("Quiz this week", waiting.title)
        assertEquals(SyncState.PENDING, waiting.sync)

        // Back online, its first send arrives but the answer is lost; the retry gets the same one back.
        server.online = true
        server.loseNextAnswer = true
        assertEquals(1, repo.sendPending().waiting)
        assertEquals(0, repo.sendPending().waiting)
        assertEquals("stored once", 1, server.rows.size)
        val stored = repo.observeReminders("p1").first().single()
        assertEquals(server.rows.single().id, stored.id)
        assertEquals(null, stored.sync)
    }

    @Test
    fun aRefresh_bringsTheParty_andWhatsMine_notOthersOwn() = runBlocking {
        party("p1")
        server.seed("p1", "bram", quiz)
        server.seed("p1", "bram", exam.copy(title = "Bram's own", audience = ReminderAudience.ME))
        server.seed("p1", "aria", exam.copy(title = "My own", audience = ReminderAudience.ME))

        assertEquals(SupabaseResult.Success(Unit), repo.refresh("p1"))
        assertEquals(setOf("Quiz this week", "My own"), repo.observeReminders("p1").first().map { it.title }.toSet())
        assertEquals(2, repo.allReminders().size)

        // Gone from the server, gone from the phone.
        server.rows.removeIf { it.title == "Quiz this week" }
        repo.refresh("p1")
        assertEquals(listOf("My own"), repo.allReminders().map { it.title })
    }

    @Test
    fun refreshAll_readsOnlyTheStudyPartiesTheAccountIsIn() = runBlocking {
        party("study")
        party("darood", type = PartyType.DAROOD)
        party("left", status = MemberStatus.LEFT)
        server.seed("study", "bram", quiz)
        server.seed("left", "bram", exam)

        repo.refreshAll()
        assertEquals(listOf("study"), repo.allReminders().map { it.partyId })
        assertEquals("one fetch: the active study party's", 1, server.calls)
    }

    @Test
    fun anInvalidDraft_isNeverSaved() = runBlocking {
        party("p1")
        assertEquals(ReminderSave.Invalid(ReminderProblem.TITLE), repo.create("p1", quiz.copy(title = " x ")))
        assertEquals(ReminderSave.Invalid(ReminderProblem.WHEN), repo.create("p1", quiz.copy(weekStart = LocalDate.of(2026, 10, 20))))
        assertEquals(ReminderSave.Invalid(ReminderProblem.ALERTS), repo.create("p1", exam.copy(alertOffsets = listOf(1, 2, 3, 4, 5))))
        assertEquals(ReminderSave.Invalid(ReminderProblem.TIME_ZONE), repo.create("p1", exam.copy(tz = "Mars/Olympus")))
        assertTrue(repo.observeReminders("p1").first().isEmpty())
        assertEquals("nothing sent", 0, server.calls)
    }

    @Test
    fun editsAndDeletes_waitOffline_andOnlyTheLastEditGoes() = runBlocking {
        party("p1")
        server.seed("p1", "aria", exam)
        repo.refresh("p1")
        val mine = repo.observeReminders("p1").first().single()

        server.online = false
        assertEquals(ReminderSave.Waiting, repo.edit(mine, exam.copy(title = "Exam at 11")))
        assertEquals(ReminderSave.Waiting, repo.edit(mine, exam.copy(title = "Exam at 12", audience = ReminderAudience.ME)))
        val shown = repo.observeReminders("p1").first().single()
        assertEquals("Exam at 12", shown.title)
        assertEquals("its audience never changes", ReminderAudience.PARTY, shown.audience)

        server.online = true
        assertEquals(1, repo.sendPending().sent)
        assertEquals("Exam at 12", server.rows.single().title)

        server.online = false
        assertEquals(ReminderSave.Waiting, repo.delete(repo.observeReminders("p1").first().single()))
        assertTrue("hidden while the delete waits", repo.observeReminders("p1").first().isEmpty())
        server.online = true
        repo.sendPending()
        assertTrue(server.rows.isEmpty())
        assertTrue(repo.allReminders().isEmpty())
    }

    @Test
    fun aNewOneNotYetSent_isChangedOrDroppedOnThePhone() = runBlocking {
        party("p1")
        server.online = false
        repo.create("p1", exam)
        val waiting = repo.observeReminders("p1").first().single()
        repo.edit(waiting, exam.copy(title = "Exam moved"))
        assertEquals(listOf("Exam moved"), repo.observeReminders("p1").first().map { it.title })
        assertEquals(ReminderSave.Sent, repo.delete(repo.observeReminders("p1").first().single()))
        assertTrue(repo.observeReminders("p1").first().isEmpty())
        server.online = true
        assertEquals(0, repo.sendPending().sent)
        assertTrue("nothing ever reached the server", server.rows.isEmpty())
    }

    @Test
    fun aRefusal_isKept_untilDiscarded() = runBlocking {
        party("p1")
        server.seed("p1", "bram", exam.copy(title = "Bram's"))
        repo.refresh("p1")
        val theirs = repo.observeReminders("p1").first().single()

        assertEquals(ReminderSave.Refused(StudyRefusal.NOT_ALLOWED), repo.edit(theirs, exam.copy(title = "Mine now")))
        val shown = repo.observeReminders("p1").first().single()
        assertEquals(SyncState.FAILED, shown.sync)
        assertEquals("not sent again", 0, repo.sendPending().sent)

        val failed = db.studyReminderDao().getPending(theirs.id)!!
        repo.discard(failed.clientId)
        assertEquals("Bram's", repo.observeReminders("p1").first().single().title)
    }

    @Test
    fun aPartyNotOnThePhone_takesNothing() = runBlocking {
        assertEquals(ReminderSave.Refused(StudyRefusal.NOT_A_MEMBER), repo.create("gone", quiz))
        assertTrue(db.studyReminderDao().pendingToSend().isEmpty())
    }
}
