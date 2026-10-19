package com.pixelquest.app.integration

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.SupabaseUsernameRepository
import com.pixelquest.app.darood.data.SetUsernameResult
import com.pixelquest.app.darood.data.remote.SupabaseDaroodPartyRemoteDataSource
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.sync.SupabaseDaroodLiveSource
import com.pixelquest.app.testing.LocalSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Darood Party's data layer against a local Supabase (opt-in: PIXELQUEST_LOCAL_SUPABASE=1 with the
 * stack running, Realtime included; see docs/LOCAL_SUPABASE.md): usernames, a party, an invitation,
 * entries with their keys, the stats, and one member's entry reaching the other's phone live.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalSupabaseDaroodTest {

    @Before
    fun onlyWithTheLocalStack() = assumeTrue("Set PIXELQUEST_LOCAL_SUPABASE=1 with the local stack running", LocalSupabase.enabled)

    private val databases = mutableListOf<AppDatabase>()

    @After
    fun tearDown() {
        databases.forEach { it.close() }
    }

    /** A player's phone: their client, its own cache, and the app's repository on them. */
    private inner class Phone(val supabase: SupabaseClient, val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val usernames = SupabaseUsernameRepository(supabase.postgrest)
        val parties = DaroodPartyRepositoryImpl(
            SupabaseDaroodPartyRemoteDataSource(supabase.postgrest) { supabase.auth.currentUserOrNull()?.id },
            db.daroodDao(),
            { supabase.auth.currentUserOrNull()?.id },
            now = System::currentTimeMillis,
            zone = { java.time.ZoneId.of("Asia/Karachi") },
            live = SupabaseDaroodLiveSource(supabase),
            numbersDelayMs = 0
        )
    }

    private suspend fun phone(tag: String): Pair<Phone, String> {
        val (supabase, id) = LocalSupabase.signedInPlayer(tag)
        val phone = Phone(supabase, id)
        val name = "${tag.take(6)}_${id.take(6)}"
        assertTrue(phone.usernames.setUsername(Username.parse(name)!!).let { it is SupabaseResult.Success && it.data is SetUsernameResult.Saved })
        return phone to name
    }

    private suspend fun waitUntil(what: String, condition: suspend () -> Boolean) {
        repeat(150) {
            if (condition()) return
            delay(100)
        }
        throw AssertionError("still waiting after 15 s: $what")
    }

    @Test
    fun aPartyEndToEnd_withLiveEntries() = runBlocking {
        val (aria, _) = phone("aria")
        val (bram, bramName) = phone("bram")

        val party = (aria.parties.createParty("Friday circle", 5, 1_000) as SupabaseResult.Success).data
        assertEquals(SupabaseResult.Success(com.pixelquest.app.darood.data.remote.InviteOutcome.SENT), aria.parties.invite(party, bramName.uppercase()))
        assertEquals(SupabaseResult.Success(com.pixelquest.app.darood.data.remote.InviteOutcome.SENT), aria.parties.invite(party, "nobody_by_this_name"))

        bram.parties.refreshParties()
        assertEquals(MemberStatus.INVITED, bram.parties.observeParty(party).first()!!.myStatus)
        assertEquals(SupabaseResult.Success(MemberStatus.ACTIVE), bram.parties.respondToInvite(party, accept = true))

        // Bram's phone follows the party; once it's listening, Aria posts on hers.
        val heard = java.util.concurrent.CopyOnWriteArrayList<com.pixelquest.app.darood.sync.PartyChange>()
        val following = launch(Dispatchers.IO) { bram.parties.followParty(party).collect { heard += it } }
        waitUntil("Bram's phone listening") { com.pixelquest.app.darood.sync.PartyChange.Subscribed in heard }
        // Realtime reports the channel joined a moment before the server listens; give it that moment.
        delay(3_000)
        val posted = aria.parties.addEntry(party, 313)
        assertEquals(EntryState.SYNCED, posted.state)

        waitUntil("Aria's entry heard live on Bram's phone") {
            heard.any { it is com.pixelquest.app.darood.sync.PartyChange.EntryAdded && it.entry.id == posted.id }
        }
        waitUntil("Aria's entry on Bram's phone") { bram.parties.observeEntries(party, 50).first().any { it.id == posted.id } }
        waitUntil("the total on Bram's phone") { bram.parties.observeParty(party).first()!!.total == 313L }
        // Closing the screen stops listening cleanly.
        following.cancel()
        following.join()

        // The same key again (an answer lost on the way back) is the same entry.
        val again = SupabaseDaroodPartyRemoteDataSource(aria.supabase.postgrest) { aria.userId }
            .addEntry(party, 313, posted.localDate, posted.tz, posted.clientId!!)
        assertEquals(posted.id, (again as SupabaseResult.Success).data.id)

        aria.parties.refreshParty(party)
        val stats = aria.parties.observeMemberStats(party).first()
        assertEquals(listOf(313L), stats.map { it.total })
        assertEquals(2, aria.parties.observeMembers(party).first().count { it.status == MemberStatus.ACTIVE })

        // The rules hold for the app too: Bram can't set the milestone; 700 more finishes it.
        assertEquals(DaroodRefusal.NOT_LEADER, DaroodRefusal.of(bram.parties.setMilestone(party, 5_000)))
        bram.parties.addEntry(party, 700)
        aria.parties.refreshParties()
        assertTrue(aria.parties.observeParty(party).first()!!.isFinished)
        val late = bram.parties.addEntry(party, 1)
        assertEquals(EntryState.FAILED, late.state)
    }

    @Test
    fun theLeaderRenames_andHandsOver_thenLeaves() = runBlocking {
        val (aria, _) = phone("ariaL")
        val (bram, bramName) = phone("bramL")
        val party = (aria.parties.createParty("Friday circle", 5, null) as SupabaseResult.Success).data
        aria.parties.invite(party, bramName)
        bram.parties.refreshParties()
        bram.parties.respondToInvite(party, accept = true)

        // Row-level security keeps Bram's rename from changing anything; the app reads that as a refusal.
        assertEquals(DaroodRefusal.NOT_LEADER, DaroodRefusal.of(bram.parties.renameParty(party, "Taken over")))
        assertEquals(SupabaseResult.Success(Unit), aria.parties.renameParty(party, "  Saturday circle "))
        assertEquals("Saturday circle", aria.parties.observeParty(party).first()!!.name)

        assertEquals(DaroodRefusal.LEADER_MUST_HAND_OVER, DaroodRefusal.of(aria.parties.leaveParty(party)))
        assertEquals(DaroodRefusal.NOT_LEADER, DaroodRefusal.of(bram.parties.transferLeadership(party, bram.userId)))
        assertEquals(SupabaseResult.Success(Unit), aria.parties.transferLeadership(party, bram.userId))
        assertEquals(bram.userId, aria.parties.observeParty(party).first()!!.leaderId)
        assertEquals(SupabaseResult.Success(Unit), aria.parties.leaveParty(party))
        bram.parties.refreshParty(party)
        assertEquals(listOf(bram.userId), bram.parties.observeMembers(party).first().filter { it.status == MemberStatus.ACTIVE }.map { it.userId })
    }

    @Test
    fun theLeaderDeletesTheParty_andItsGoneForEveryone() = runBlocking {
        val (aria, _) = phone("ariaD")
        val (bram, bramName) = phone("bramD")
        val party = (aria.parties.createParty("Friday circle", 5, null) as SupabaseResult.Success).data
        aria.parties.invite(party, bramName)
        bram.parties.refreshParties()
        bram.parties.respondToInvite(party, accept = true)
        assertEquals(EntryState.SYNCED, bram.parties.addEntry(party, 300).state)

        assertEquals(DaroodRefusal.NOT_LEADER, DaroodRefusal.of(bram.parties.deleteParty(party)))
        assertEquals(SupabaseResult.Success(Unit), aria.parties.deleteParty(party))
        assertEquals(null, aria.parties.observeParty(party).first())
        bram.parties.refreshParties()
        assertEquals(null, bram.parties.observeParty(party).first())
        assertEquals(DaroodRefusal.PARTY_CLOSED, DaroodRefusal.of(
            SupabaseDaroodPartyRemoteDataSource(bram.supabase.postgrest) { bram.userId }
                .addEntry(party, 1, java.time.LocalDate.now(java.time.ZoneId.of("Asia/Karachi")), "Asia/Karachi", java.util.UUID.randomUUID().toString())
        ))
    }
}
