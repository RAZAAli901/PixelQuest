package com.pixelquest.app.study.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.remote.StudyItemDraft
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.study.sync.StudySyncScheduler
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A study party on the phone: the cache, ticks and study time saved first and sent once, and the week. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyRepositoryTest {

    private val server = FakeDaroodServer(me = "aria").apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T10:00:00Z")
    }
    private val study = FakeStudyServer(server)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private var scheduled = 0
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { ZoneId.of("UTC") })
    private val repo = StudyRepositoryImpl(
        study, db.studyDao(), db.partyDao(), { server.me }, { server.now.toEpochMilli() }, { ZoneId.of("UTC") }, StudySyncScheduler { scheduled++ }
    )
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)

    @After
    fun tearDown() = db.closeAfterWrites()

    private fun onThePhone() = runBlocking {
        parties.refreshParties()
        parties.refreshParty(party)
    }

    @Test
    fun aRefresh_bringsTheParty_withUsernamesOnTheBoard() = runBlocking {
        onThePhone()
        val quiz = study.seedItem(party, "bram", "Waves quiz", StudyItemKind.QUIZ, dueAt = server.now.plus(Duration.ofDays(3)))
        study.seedItem(party, "aria", "Lab report")
        study.seedCompletion(quiz.id, "bram")
        study.seedSession(party, "bram", 25)
        study.seedSession(party, "bram", 40)

        assertEquals(SupabaseResult.Success(Unit), repo.refresh(party))
        assertEquals(listOf("Waves quiz", "Lab report"), repo.observeItems(party).first().map { it.title })
        val board = repo.observeScores(party, repo.thisWeek()).first()
        assertEquals("bram", board.single().username)
        assertEquals(15 + 5 + 6, board.single().points)
        assertEquals(2, repo.observeSessions(party, repo.thisWeek()).first().size)
    }

    @Test
    fun aTickOffline_waits_thenGoesOnce_evenIfItsAnswerIsLost() = runBlocking {
        onThePhone()
        val item = study.seedItem(party, "bram", "Lab report")
        repo.refresh(party)

        server.offline = true
        val waiting = repo.complete(item)
        assertEquals(SyncState.PENDING, waiting.state)
        assertEquals(1, scheduled)
        assertEquals(waiting.clientId, repo.complete(item).clientId)
        assertEquals(listOf(SyncState.PENDING), repo.observeCompletions(party).first().map { it.state })

        server.offline = false
        study.loseNextAnswer = true
        assertTrue(repo.sendPending().needsRetry)
        assertEquals(0, repo.sendPending().waiting)
        assertEquals(1, study.completions.size)
        val stored = repo.observeCompletions(party).first().single()
        assertEquals(SyncState.SYNCED, stored.state)
        assertEquals(waiting.clientId, stored.clientId)
        assertEquals(10, repo.observeScores(party, repo.thisWeek()).first().single().points)
    }

    @Test
    fun undo_dropsAWaitingTick_takesBackASentOne_andNotAfter24Hours() = runBlocking {
        onThePhone()
        val item = study.seedItem(party, "bram", "Lab report")
        repo.refresh(party)

        server.offline = true
        repo.complete(item)
        assertEquals(SupabaseResult.Success(Unit), repo.undo(item.id))
        assertTrue(repo.observeCompletions(party).first().isEmpty())
        assertTrue(study.completions.isEmpty())

        server.offline = false
        assertEquals(SyncState.SYNCED, repo.complete(item).state)
        assertEquals(SupabaseResult.Success(Unit), repo.undo(item.id))
        assertTrue(study.completions.isEmpty())

        repo.complete(item)
        server.now = server.now.plus(Duration.ofHours(25))
        val calls = study.calls.size
        assertEquals(StudyRefusal.NOT_ALLOWED, StudyRefusal.of(repo.undo(item.id)))
        assertEquals("checked on the phone first", calls, study.calls.size)
        assertEquals(1, study.completions.size)
    }

    @Test
    fun aRefusedTick_isKeptWithItsReason_andCanBeDropped() = runBlocking {
        onThePhone()
        val item = study.seedItem(party, "bram", "Lab report")
        repo.refresh(party)
        study.items.remove(item.id)

        val refused = repo.complete(item)
        assertEquals(SyncState.FAILED, refused.state)
        assertEquals(StudyRefusal.NO_SUCH_ITEM.token, db.studyDao().getPending(refused.clientId!!)?.refusal)
        assertEquals(1, repo.retry(refused.clientId!!).refused)
        repo.discard(refused.clientId!!)
        assertTrue(repo.observeCompletions(party).first().isEmpty())
    }

    @Test
    fun studyTime_isSavedThenSent_andCountedPerDay() = runBlocking {
        onThePhone()
        server.offline = true
        assertEquals(SyncState.PENDING, repo.logStudy(party, 25).state)
        server.offline = false
        assertEquals(SyncState.SYNCED, repo.logStudy(party, 200).state)
        assertEquals(listOf(25, 200), study.sessions.map { it.minutes })
        assertEquals(12, repo.observeScores(party, repo.thisWeek()).first().single().studyPoints)
    }

    @Test
    fun itemsChange_onlyAsTheServerAllows() = runBlocking {
        onThePhone()
        val added = (repo.addItem(party, StudyItemDraft(StudyItemKind.ASSIGNMENT, "Essay", null, null, null), pinned = true) as SupabaseResult.Success).data
        assertTrue(repo.observeItem(added.id).first()!!.pinned)

        server.me = "bram"
        assertEquals(StudyRefusal.NOT_ALLOWED, StudyRefusal.of(repo.editItem(added.id, StudyItemDraft(StudyItemKind.ASSIGNMENT, "Mine", null, null, null))))
        assertEquals("Essay", repo.observeItem(added.id).first()!!.title)

        server.me = "aria"
        repo.setPinned(added.id, false)
        assertEquals(false, repo.observeItem(added.id).first()!!.pinned)
        repo.deleteItem(added.id)
        assertEquals(null, repo.observeItem(added.id).first())
    }

    @Test
    fun lastWeek_isClosed_onceItHasEndedEverywhere() = runBlocking {
        onThePhone()
        val item = study.seedItem(party, "aria", "Lab report")
        study.seedCompletion(item.id, "bram", LocalDate.of(2026, 10, 21))
        // Monday 26 October: last week is not over everywhere yet.
        server.now = Instant.parse("2026-10-26T10:00:00Z")
        repo.refresh(party)
        assertTrue(repo.observeWinners(party).first().isEmpty())
        // Tuesday 27 October: it is.
        server.now = Instant.parse("2026-10-27T10:00:00Z")
        repo.refresh(party)
        val winner = repo.observeWinners(party).first().single()
        assertEquals(LocalDate.of(2026, 10, 19), winner.weekStart)
        assertEquals("bram", winner.username)
        assertNotNull(repo.observeAllScores(party).first().firstOrNull { it.weekStart == winner.weekStart })
    }
}
