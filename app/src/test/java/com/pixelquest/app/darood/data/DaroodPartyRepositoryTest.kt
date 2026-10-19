package com.pixelquest.app.darood.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The repository on the real cache and a fake server: what the screens see, online and off. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodPartyRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "me").apply {
        usernames += mapOf("me" to "aria_k", "u2" to "bram", "u3" to "cleo")
    }
    private var scheduled = 0
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, { 1_000L }, { java.time.ZoneId.of("Asia/Karachi") }) { scheduled++ }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun myParties_comeFromTheServer_withTheirNumbers_andStayOffline() = runBlocking {
        val friday = server.seedParty("Friday circle", "me", "u2", milestone = 1000)
        server.seedEntry(friday, "u2", 300)
        val family = server.seedParty("Family", "u3")
        server.members += com.pixelquest.app.darood.data.remote.PartyMemberDto(family, "me", "member", "invited", null, null)

        assertEquals(SupabaseResult.Success(Unit), repository.refreshParties())

        val parties = repository.observeMyParties().first()
        assertEquals(listOf("Family", "Friday circle"), parties.map { it.name })
        assertEquals(MemberStatus.INVITED, parties[0].myStatus)
        val mine = parties[1]
        assertEquals(300L, mine.total)
        assertEquals(2, mine.memberCount)
        assertEquals(0.3f, mine.progress!!, 0.001f)

        // Offline: the refresh fails, and the parties are still there.
        server.offline = true
        assertTrue(repository.refreshParties() is SupabaseResult.NetworkError)
        assertEquals(2, repository.observeMyParties().first().size)
    }

    @Test
    fun aParty_bringsItsMembers_theirNumbers_andItsNewestEntries() = runBlocking {
        val friday = server.seedParty("Friday circle", "me", "u2")
        server.seedEntry(friday, "u2", 300)
        server.seedEntry(friday, "me", 100)
        server.seedEntry(friday, "u2", 33)

        repository.refreshParty(friday)

        val members = repository.observeMembers(friday).first()
        assertEquals(listOf("aria_k" to PartyRole.LEADER, "bram" to PartyRole.MEMBER), members.map { it.username to it.role })
        assertEquals(listOf(333L, 100L), repository.observeMemberStats(friday).first().map { it.total })
        assertEquals(listOf(33, 100, 300), repository.observeEntries(friday, 50).first().map { it.amount })
    }

    @Test
    fun olderEntries_loadAPageAtATime() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        repeat(120) { server.seedEntry(friday, "me", 1) }

        repository.refreshParty(friday)
        assertEquals(50, repository.observeEntries(friday, 500).first().size)

        assertEquals(SupabaseResult.Success(true), repository.loadOlderEntries(friday))
        assertEquals(SupabaseResult.Success(false), repository.loadOlderEntries(friday))
        assertEquals(120, repository.observeEntries(friday, 500).first().size)
    }

    @Test
    fun changes_refreshWhatTheyChanged() = runBlocking {
        val created = (repository.createParty("  Night readers ", 12, null) as SupabaseResult.Success).data
        assertEquals("Night readers", repository.observeParty(created).first()!!.name)

        assertEquals(SupabaseResult.Success(1500L), repository.setMilestone(created, 1500))
        assertEquals(1500L, repository.observeParty(created).first()!!.milestone)

        val theirs = server.seedParty("Family", "u3", "me")
        repository.refreshParties()
        repository.leaveParty(theirs)
        assertNull("a party left is gone from the list", repository.observeParty(theirs).first())
    }

    @Test
    fun aRefusal_leavesTheCacheAsItWas() = runBlocking {
        val theirs = server.seedParty("Family", "u3", "me")
        repository.refreshParties()

        val result = repository.setMilestone(theirs, 100)

        assertEquals(com.pixelquest.app.darood.domain.DaroodRefusal.NOT_LEADER, com.pixelquest.app.darood.domain.DaroodRefusal.of(result))
        assertNull(repository.observeParty(theirs).first()!!.milestone)
    }

    @Test
    fun anEntry_isSentAtOnce_whenItCanBe() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        repository.refreshParties()

        val entry = repository.addEntry(friday, 313)

        assertEquals(com.pixelquest.app.darood.domain.EntryState.SYNCED, entry.state)
        assertEquals("nothing left for the worker", 0, scheduled)
        assertTrue(entry.id != null)
        assertEquals("Asia/Karachi", server.entries.single().tz)
        assertEquals(listOf(313), repository.observeEntries(friday, 50).first().map { it.amount })
        assertEquals(0, repository.observePendingCount().first())
        assertEquals("the party's total followed", 313L, repository.observeParty(friday).first()!!.total)
    }

    @Test
    fun offline_anEntryWaits_onTopAndMine_thenGoesWhenItCan() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        server.seedEntry(friday, "me", 100)
        repository.refreshParty(friday)
        server.offline = true

        val first = repository.addEntry(friday, 33)
        val second = repository.addEntry(friday, 11)

        assertEquals(com.pixelquest.app.darood.domain.EntryState.PENDING, first.state)
        assertEquals("the sync worker is asked to send them", 2, scheduled)
        val shown = repository.observeEntries(friday, 50).first()
        assertEquals("waiting ones first, newest on top", listOf(11, 33, 100), shown.map { it.amount })
        assertEquals(listOf("me", "me"), shown.take(2).map { it.userId })
        assertEquals(2, repository.observePendingCount().first())
        assertEquals(SendReport(0, 0, 2), repository.sendPending())

        server.offline = false
        assertEquals(SendReport(2, 0, 0), repository.sendPending())
        assertEquals("sent in the order posted", listOf(100, 33, 11), server.entries.map { it.amount })
        assertEquals(0, repository.observePendingCount().first())
    }

    @Test
    fun aRefusedEntry_isMarked_andTheRestStillGo() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        repository.refreshParties()
        server.offline = true
        repeat(5) { repository.addEntry(friday, 100_000) }
        repository.addEntry(friday, 7)
        server.offline = false

        val report = repository.sendPending()

        // The day's 500,000 is reached by the fifth; the sixth, 7 more, is refused.
        assertEquals(SendReport(5, 1, 0), report)
        val refused = repository.observeEntries(friday, 50).first().first()
        assertEquals(com.pixelquest.app.darood.domain.EntryState.FAILED, refused.state)
        assertEquals(7, refused.amount)
        assertEquals("a refused one isn't sent again as it is", SendReport(0, 0, 0), repository.sendPending())
    }
}
