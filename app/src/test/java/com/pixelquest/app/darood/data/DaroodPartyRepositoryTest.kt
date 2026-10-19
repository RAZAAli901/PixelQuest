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
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao()) { 1_000L }

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
}
