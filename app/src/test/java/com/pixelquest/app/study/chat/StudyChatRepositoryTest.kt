package com.pixelquest.app.study.chat

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.data.local.PartyEntity
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.data.ChatSend
import com.pixelquest.app.study.chat.data.StudyChatRepositoryImpl
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.testing.FakeChatServer
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

/** A study party's chat on the phone: opens offline, sends once, pages back, and keeps refusals. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyChatRepositoryTest {

    private val server = FakeChatServer(me = "aria").apply { members["p1"] = mutableSetOf("aria", "bram"); leaders["p1"] = "bram" }
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private var sendsAsked = 0
    private val repo = StudyChatRepositoryImpl(server, db.chatDao(), { server.me }, clock = { server.now.toEpochMilli() }) { sendsAsked++ }

    private fun party() = runBlocking {
        db.partyDao().upsertParties(listOf(
            PartyEntity(
                id = "p1", name = "p1", leaderId = "bram", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
                createdAt = 0, total = 0, entryCount = 0, memberCount = 2, lastEntryDate = null, myRole = PartyRole.MEMBER,
                myStatus = MemberStatus.ACTIVE, syncedAt = 0, partyType = PartyType.STUDY
            )
        ))
    }

    @After
    fun tearDown() = db.closeAfterWrites()

    private suspend fun shown() = repo.observeMessages("p1", 100).first()

    @Test
    fun aRefresh_bringsTheNewestPage_thenWhatsNew_andOpensOffline() = runBlocking {
        party()
        repeat(60) { server.seed("p1", "bram", "Old $it", minutesAgo = 120L - it) }
        assertEquals(SupabaseResult.Success(Unit), repo.refresh("p1"))
        assertEquals("the newest page first", 50, shown().size)
        assertEquals("Old 59", shown().last().body)

        server.seed("p1", "bram", "Just now")
        repo.refresh("p1")
        assertEquals("Just now", shown().last().body)

        // Back further: a page of what came before, then nothing more.
        assertEquals(SupabaseResult.Success(false), repo.loadOlder("p1"))
        assertEquals(61, shown().size)

        server.online = false
        assertEquals("still there offline", 61, shown().size)
    }

    @Test
    fun aMessageOffline_showsAtOnce_thenGoesOnce_evenIfItsAnswerIsLost() = runBlocking {
        party()
        server.online = false
        assertEquals(ChatSend.Waiting, repo.send("p1", "  Anyone done Q3?  "))
        assertEquals("asked to go once there's a connection", 1, sendsAsked)
        val waiting = shown().single()
        assertEquals("Anyone done Q3?", waiting.body)
        assertEquals(SyncState.PENDING, waiting.sync)

        server.online = true
        server.loseNextAnswer = true
        assertEquals(1, repo.sendPending().waiting)
        assertEquals(0, repo.sendPending().waiting)
        assertEquals("stored once", 1, server.messages.size)
        val sent = shown().single()
        assertEquals(server.messages.single().id, sent.id)
        assertEquals(null, sent.sync)
    }

    @Test
    fun whatTheServerWontTake_isKept_untilRetriedOrDiscarded() = runBlocking {
        party()
        repeat(30) { server.seed("p1", "aria", "Spam $it") }
        assertEquals(ChatSend.Refused(StudyRefusal.TOO_MANY_MESSAGES), repo.send("p1", "One too many"))
        val failed = shown().single { it.sync == SyncState.FAILED }
        assertEquals("not sent again on its own", 0, repo.sendPending().sent)

        server.messages.clear()
        assertEquals(ChatSend.Sent, repo.retry(failed.clientId!!))
        assertEquals(listOf("One too many"), server.messages.map { it.body })

        server.messages.clear()
        repeat(30) { server.seed("p1", "aria", "Spam $it") }
        repo.send("p1", "Gone")
        repo.discard(shown().single { it.sync == SyncState.FAILED }.clientId!!)
        assertTrue(shown().none { it.body == "Gone" })
    }

    @Test
    fun emptyOrTooLong_isNeverSaved_andNotInAPartyOffThePhone() = runBlocking {
        party()
        assertEquals(ChatSend.Invalid, repo.send("p1", "   "))
        assertEquals(ChatSend.Invalid, repo.send("p1", "x".repeat(2001)))
        assertEquals(ChatSend.Refused(StudyRefusal.NOT_A_MEMBER), repo.send("gone", "Hi"))
        assertEquals(0, server.calls)
        assertEquals(ChatSend.Sent, repo.send("p1", "x".repeat(2000)))
    }

    @Test
    fun deleting_isTheSendersOrTheLeaders_andStaysAsAPlace() = runBlocking {
        party()
        val bramS = server.seed("p1", "bram", "Bram's")
        repo.refresh("p1")
        assertEquals(StudyRefusal.result(StudyRefusal.NOT_ALLOWED), repo.delete(bramS.id))
        repo.send("p1", "Mine")
        val mine = shown().single { it.body == "Mine" }
        assertEquals(SupabaseResult.Success(Unit), repo.delete(mine.id))
        val gone = shown().single { it.id == mine.id }
        assertTrue(gone.deleted)
        assertEquals(null, gone.body)

        // Deleted elsewhere: a refresh brings it as deleted.
        server.me = "bram"
        server.messages.replaceAll { if (it.id == bramS.id) it.copy(body = null, deletedAt = server.now) else it }
        server.me = "aria"
        repo.refresh("p1")
        assertTrue(shown().single { it.id == bramS.id }.deleted)
    }

    @Test
    fun theReadMark_isKeptHere_andOnTheServer() = runBlocking {
        party()
        repo.markRead("p1")
        assertEquals(server.now, repo.observeLastRead("p1").first())
        assertEquals(server.now, server.reads["p1" to "aria"])
    }
}
