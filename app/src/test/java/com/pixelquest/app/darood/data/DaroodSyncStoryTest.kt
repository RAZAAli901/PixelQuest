package com.pixelquest.app.darood.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Entries posted with a bad connection, end to end on the real cache and a fake server: whatever
 * happens to the connection, every entry reaches the server once, in the order it was posted, and
 * the phone shows each one once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodSyncStoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "me").apply { usernames += mapOf("me" to "aria_k", "u2" to "bram") }
    private var workerAsked = 0
    private val repository = DaroodPartyRepositoryImpl(
        server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("Asia/Karachi") },
        syncScheduler = { workerAsked++ }
    )

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun shown(party: String) = repository.observeEntries(party, 500).first()

    @Test
    fun offline_aDayOfEntries_waits_thenEachReachesTheServerOnce_inOrder() = runBlocking {
        val party = server.seedParty("Friday circle", "me", "u2")
        repository.refreshParty(party)
        server.offline = true

        val amounts = listOf(33, 100, 313, 11, 1000)
        amounts.forEach { repository.addEntry(party, it) }

        assertEquals(amounts.reversed(), shown(party).map { it.amount })
        assertTrue(shown(party).all { it.state == EntryState.PENDING })
        assertEquals("the worker was asked each time", 5, workerAsked)
        assertEquals(0, server.entries.size)

        // The worker runs while still offline: nothing lost, nothing sent.
        assertTrue(repository.sendPending().needsRetry)
        assertEquals(5, repository.observePendingCount().first())

        server.offline = false
        assertEquals(SendReport(5, 0, 0), repository.sendPending())
        assertEquals(amounts, server.entries.map { it.amount })
        assertEquals(1457L, server.total(party))
        assertTrue(shown(party).all { it.state == EntryState.SYNCED })
        assertEquals("each shown once", 5, shown(party).size)
    }

    @Test
    fun aFlakyConnection_thatLosesAnswers_neverCountsAnEntryTwice() = runBlocking {
        val party = server.seedParty("Friday circle", "me")
        repository.refreshParties()

        server.loseNextAnswer = true
        repository.addEntry(party, 300)
        server.loseNextAnswer = true
        assertTrue("the retry's answer is lost too", repository.sendPending().needsRetry)
        assertEquals(SendReport(1, 0, 0), repository.sendPending())

        assertEquals(1, server.entries.size)
        assertEquals(300L, server.total(party))
        assertEquals(1, shown(party).size)
    }

    @Test
    fun twoSendsAtOnce_theWorkerAndAPull_sendEachEntryOnce() = runBlocking {
        val party = server.seedParty("Friday circle", "me")
        repository.refreshParties()
        server.offline = true
        repeat(4) { repository.addEntry(party, 10) }
        server.offline = false

        listOf(async { repository.sendPending() }, async { repository.sendPending() }).awaitAll()

        assertEquals(4, server.entries.size)
        assertEquals(40L, server.total(party))
        assertEquals(0, repository.observePendingCount().first())
        assertEquals(4, shown(party).size)
    }

    @Test
    fun signedOutMidway_theRestWait_ratherThanBeingRefused() = runBlocking {
        val party = server.seedParty("Friday circle", "me")
        repository.refreshParties()
        server.offline = true
        repository.addEntry(party, 1)
        repository.addEntry(party, 2)
        server.offline = false
        server.me = null

        assertEquals(SendReport(0, 0, 2), repository.sendPending())
        assertTrue(shown(party).all { it.state == EntryState.PENDING })

        server.me = "me"
        assertEquals(SendReport(2, 0, 0), repository.sendPending())
    }

    @Test
    fun entriesForSeveralParties_keepTheirOrder() = runBlocking {
        val friday = server.seedParty("Friday circle", "me")
        val family = server.seedParty("Family", "me")
        repository.refreshParties()
        server.offline = true
        repository.addEntry(friday, 1)
        repository.addEntry(family, 2)
        repository.addEntry(friday, 3)
        server.offline = false

        repository.sendPending()

        assertEquals(listOf(friday to 1, family to 2, friday to 3), server.entries.map { it.partyId to it.amount })
    }
}
