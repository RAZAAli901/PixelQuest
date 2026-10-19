package com.pixelquest.app.darood.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.PartyRole
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
import java.time.LocalDate

/** Darood Party's cache in the real database: refreshes, pending entries, and clearing it all. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodDaoTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.daroodDao()
    private val day = LocalDate.of(2026, 10, 19)

    @After
    fun tearDown() {
        db.close()
    }

    private fun party(id: String, name: String, status: MemberStatus = MemberStatus.ACTIVE) = PartyEntity(
        id = id, name = name, leaderId = "me", maxMembers = 10, milestone = null, milestoneSetAt = null, completedAt = null,
        createdAt = 0, total = 0, entryCount = 0, memberCount = 1, lastEntryDate = null,
        myRole = PartyRole.MEMBER, myStatus = status, syncedAt = 0
    )

    private fun member(partyId: String, userId: String) =
        PartyMemberEntity(partyId, userId, "user_$userId", PartyRole.MEMBER, MemberStatus.ACTIVE, null, null)

    private fun pending(clientId: String, partyId: String = "p1", at: Long = 0) =
        PendingEntryEntity(clientId, partyId, 300, day, "UTC", at)

    @Test
    fun invitations_comeFirst_thenParties_byName() = runBlocking {
        dao.replaceParties(listOf(party("p1", "zikr circle"), party("p2", "Family", MemberStatus.INVITED), party("p3", "Aunts")))

        assertEquals(listOf("Family", "Aunts", "zikr circle"), dao.observeParties().first().map { it.name })
    }

    @Test
    fun aRefresh_dropsPartiesNoLongerListed_butKeepsWhatBelongsToTheRest() = runBlocking {
        dao.replaceParties(listOf(party("p1", "Friday"), party("p2", "Family")))
        dao.replaceMembers("p1", listOf(member("p1", "u1")))
        dao.upsertEntries(listOf(DaroodEntryEntity(1, "p1", "u1", 33, day, "UTC", 0)))
        dao.insertPending(pending("c1"))

        // p1 again (its numbers changed), p2 gone.
        dao.replaceParties(listOf(party("p1", "Friday").copy(total = 33)))

        assertEquals(listOf("p1"), dao.observeParties().first().map { it.id })
        assertEquals(33L, dao.getParty("p1")!!.total)
        assertEquals("upserting kept its members", 1, dao.observeMembers("p1").first().size)
        assertEquals(1, dao.entryCount("p1"))
        assertEquals(1, dao.observePending("p1").first().size)
    }

    @Test
    fun pendingEntries_goInTheOrderPosted_andAreSwappedForTheServersCopy() = runBlocking {
        dao.replaceParties(listOf(party("p1", "Friday")))
        dao.insertPending(pending("c2", at = 2))
        dao.insertPending(pending("c1", at = 1))
        dao.insertPending(pending("c3", at = 3))
        dao.setPendingState("c3", EntryState.FAILED, "daily_limit")

        assertEquals(listOf("c1", "c2"), dao.pendingToSend().map { it.clientId })
        assertEquals(2, dao.observePendingCount().first())

        dao.countAttempt("c1")
        assertEquals(1, dao.getPending("c1")!!.attempts)
        dao.confirmSent("c1", DaroodEntryEntity(42, "p1", "me", 300, day, "UTC", 5, clientId = "c1"))

        assertNull(dao.getPending("c1"))
        assertEquals(listOf(42L), dao.observeEntries("p1", 50).first().map { it.id })
        assertEquals("daily_limit", dao.getPending("c3")!!.refusal)
    }

    @Test
    fun entries_pageNewestFirst() = runBlocking {
        dao.replaceParties(listOf(party("p1", "Friday")))
        dao.upsertEntries((1L..60L).map { DaroodEntryEntity(it, "p1", "u1", 1, day, "UTC", it) })

        val newest = dao.observeEntries("p1", 50).first()
        assertEquals(50, newest.size)
        assertEquals(60L, newest.first().id)
        assertEquals(1L, dao.oldestEntryId("p1"))
    }

    @Test
    fun signingOut_clearsEverything() = runBlocking {
        dao.replaceParties(listOf(party("p1", "Friday")))
        dao.replaceMembers("p1", listOf(member("p1", "u1")))
        dao.upsertEntries(listOf(DaroodEntryEntity(1, "p1", "u1", 33, day, "UTC", 0)))
        dao.insertPending(pending("c1"))

        dao.clearAll()

        assertTrue(dao.observeParties().first().isEmpty())
        assertTrue(dao.observeMembers("p1").first().isEmpty())
        assertEquals(0, dao.entryCount("p1"))
        assertTrue(dao.pendingToSend().isEmpty())
    }
}
