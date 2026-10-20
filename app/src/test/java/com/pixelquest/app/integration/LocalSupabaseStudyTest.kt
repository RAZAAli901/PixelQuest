package com.pixelquest.app.integration

import android.app.Application
import com.pixelquest.app.darood.data.SetUsernameResult
import com.pixelquest.app.darood.data.SupabaseUsernameRepository
import com.pixelquest.app.darood.data.remote.SupabaseDaroodPartyRemoteDataSource
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.remote.StudyItemDraft
import com.pixelquest.app.study.data.remote.SupabaseStudyRemoteDataSource
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.weekStartOf
import com.pixelquest.app.testing.LocalSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * A study party's data source against a local Supabase (opt-in: PIXELQUEST_LOCAL_SUPABASE=1 with the
 * stack running): items, ticks and study time with their keys, who may change what, and the week.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalSupabaseStudyTest {

    @Before
    fun onlyWithTheLocalStack() = assumeTrue("Set PIXELQUEST_LOCAL_SUPABASE=1 with the local stack running", LocalSupabase.enabled)

    private class Player(val id: String, val name: String, val parties: SupabaseDaroodPartyRemoteDataSource, val study: SupabaseStudyRemoteDataSource)

    private suspend fun player(tag: String): Player {
        val (supabase, id) = LocalSupabase.signedInPlayer(tag)
        val name = "${tag.take(6)}_${id.take(6)}"
        val saved = SupabaseUsernameRepository(supabase.postgrest).setUsername(Username.parse(name)!!)
        assertTrue(saved is SupabaseResult.Success && saved.data is SetUsernameResult.Saved)
        val account = { supabase.auth.currentUserOrNull()?.id }
        return Player(id, name, SupabaseDaroodPartyRemoteDataSource(supabase.postgrest, account), SupabaseStudyRemoteDataSource(supabase.postgrest, account))
    }

    private fun <T> SupabaseResult<T>.ok(): T = (this as? SupabaseResult.Success)?.data ?: throw AssertionError("not a success: $this")

    @Test
    fun aStudyWeek_throughTheDataSource() = runBlocking {
        val aria = player("aria")
        val bram = player("bram")
        val party = aria.parties.createParty("Physics group", 5, PartyType.STUDY).ok()
        aria.parties.invite(party, bram.name).ok()
        assertEquals(MemberStatus.ACTIVE, bram.parties.respondToInvite(party, accept = true).ok())

        // Aria adds a quiz due in three days, pinned (she leads); Bram an assignment with no date.
        val zone = ZoneId.of("Asia/Karachi")
        val today = LocalDate.now(zone)
        val due = Instant.now().plus(Duration.ofDays(3))
        val quiz = aria.study.addItem(party, StudyItemDraft(StudyItemKind.QUIZ, "  Waves quiz ", "Physics", null, due), pinned = true).ok()
        assertEquals("Waves quiz", quiz.title)
        assertTrue(quiz.pinned)
        assertEquals(aria.id, quiz.createdBy)
        val essay = bram.study.addItem(party, StudyItemDraft(StudyItemKind.ASSIGNMENT, "Lab write-up", null, "", null)).ok()
        assertEquals(null, essay.notes)
        assertEquals(listOf(quiz.id, essay.id), aria.study.fetchItems(party).ok().map { it.id })

        // Bram may not change Aria's item, nor pin his own; Aria, the leader, may change his.
        assertEquals(StudyRefusal.NOT_ALLOWED, StudyRefusal.of(bram.study.editItem(quiz.id, StudyItemDraft(StudyItemKind.QUIZ, "Mine now", null, null, due))))
        assertEquals(StudyRefusal.NOT_LEADER, StudyRefusal.of(bram.study.setPinned(essay.id, true)))
        assertEquals("Lab report", aria.study.editItem(essay.id, StudyItemDraft(StudyItemKind.ASSIGNMENT, "Lab report", null, null, null)).ok().title)

        // Bram ticks the quiz off; the same key again, or another phone's tick, is the same one.
        val key = UUID.randomUUID().toString()
        val done = bram.study.complete(quiz.id, today, zone.id, key).ok()
        assertEquals(party, done.partyId)
        assertEquals(done.id, bram.study.complete(quiz.id, today, zone.id, key).ok().id)
        assertEquals(done.id, bram.study.complete(quiz.id, today, zone.id, UUID.randomUUID().toString()).ok().id)
        assertEquals(StudyRefusal.BAD_DAY, StudyRefusal.of(bram.study.complete(essay.id, today.minusDays(30), zone.id, UUID.randomUUID().toString())))

        // Aria can't undo it for him; he can.
        assertEquals(StudyRefusal.NOT_ALLOWED, StudyRefusal.of(aria.study.undoCompletion(done.id!!)))
        bram.study.undoCompletion(done.id!!).ok()
        val again = bram.study.complete(quiz.id, today, zone.id, UUID.randomUUID().toString()).ok()

        // Study time: 25 and 40 minutes, the first sent twice, make 65 minutes and 6 points.
        val first = UUID.randomUUID().toString()
        val session = bram.study.logSession(party, 25, today, zone.id, first).ok()
        assertEquals(session.id, bram.study.logSession(party, 25, today, zone.id, first).ok().id)
        bram.study.logSession(party, 40, today, zone.id, UUID.randomUUID().toString()).ok()
        assertEquals(2, aria.study.fetchSessions(party, today.minusDays(7)).ok().size)

        val week = weekStartOf(today)
        val score = aria.study.fetchScores(party, week).ok().single { it.userId == bram.id }
        assertEquals(week, score.weekStart)
        assertEquals(15 + 5 + 6, score.points)
        assertEquals(65, score.studyMinutes)
        assertEquals(1, aria.study.fetchCompletions(party).ok().count { it.id == again.id })

        // This week isn't over; nothing recorded yet. Aria deletes his item, as the leader may.
        assertEquals(StudyRefusal.WEEK_NOT_OVER, StudyRefusal.of(aria.study.closeWeek(party, week)))
        assertEquals(emptyList<Any>(), aria.study.fetchWinners(party).ok())
        aria.study.deleteItem(essay.id).ok()
        assertEquals(listOf(quiz.id), bram.study.fetchItems(party).ok().map { it.id })
    }
}
