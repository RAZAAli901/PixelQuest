package com.pixelquest.app.party.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.data.remote.InviteOutcome
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every party's repository, on its own: a study party starts, invites, is joined and left through it
 * as a Darood Party is, and only a Darood Party's refresh brings entries (Darood's repository, built
 * on this one).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PartyRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "me").apply {
        usernames["me"] = "aria_k"
        usernames["sam"] = "sam_w"
    }
    private val parties = PartyRepositoryImpl(server, db.partyDao()) { 1_000L }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun aStudyParty_starts_andIsCachedAsOne() = runBlocking {
        val made = parties.createParty("  Physics group ", 6, PartyType.STUDY) as SupabaseResult.Success

        val party = parties.observeParty(made.data).first()!!
        assertEquals("Physics group", party.name)
        assertEquals(PartyType.STUDY, party.type)
        assertEquals(6, party.maxMembers)
        assertEquals(null, party.milestone)
        assertEquals(MemberStatus.ACTIVE, party.myStatus)
    }

    @Test
    fun aStudyParty_isJoined_andLeft_asADaroodPartyIs() = runBlocking {
        val id = server.seedParty("Exam group", "sam", type = PartyType.STUDY)
        server.me = "sam"
        assertEquals(SupabaseResult.Success(InviteOutcome.SENT), parties.invite(id, "aria_k"))

        server.me = "me"
        parties.refreshParties()
        assertEquals(MemberStatus.INVITED, parties.observeParty(id).first()!!.myStatus)
        assertEquals(SupabaseResult.Success(MemberStatus.ACTIVE), parties.respondToInvite(id, accept = true))
        assertEquals(listOf("sam_w", "aria_k"), parties.observeMembers(id).first().map { it.username })

        assertTrue(parties.leaveParty(id) is SupabaseResult.Success)
        assertEquals(null, parties.observeParty(id).first())
    }

    @Test
    fun onlyADaroodParty_bringsEntriesWhenItRefreshes() = runBlocking {
        val study = server.seedParty("Exam group", "me", "sam", type = PartyType.STUDY)
        val darood = server.seedParty("Friday circle", "me", "sam")
        server.seedEntry(darood, "sam", 313)
        val both = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })

        server.calls.clear()
        both.refreshParty(study)
        assertFalse("a study party has no entries to fetch", "fetchEntries" in server.calls)
        assertEquals(2, both.observeMembers(study).first().size)

        both.refreshParty(darood)
        assertTrue("fetchEntries" in server.calls)
        assertEquals(listOf(313), both.observeEntries(darood, 10).first().map { it.amount })
    }
}
