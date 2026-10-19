package com.pixelquest.app.darood.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.backup.BackupPayload
import com.pixelquest.app.data.backup.DataExportImport
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Darood Party's data is the signed-in account's alone, and never in the game's backups. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodAccountCleanupTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "aria").apply { usernames["aria"] = "aria_k" }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })
    private val auth = FakeAuthRepository()
    private val owner = object : DaroodCacheOwner {
        var id: String? = null
        override fun owner() = id
        override fun setOwner(accountId: String?) {
            id = accountId
        }
    }
    private val cleanup = DaroodAccountCleanup(auth, db.daroodDao(), owner)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun fillCache() {
        val party = server.seedParty("Friday circle", "aria")
        repository.refreshParty(party)
        server.offline = true
        repository.addEntry(party, 33)
        server.offline = false
    }

    private suspend fun parties() = repository.observeMyParties().first().size

    private fun waitFor(condition: suspend () -> Boolean) = runBlocking {
        repeat(100) {
            if (condition()) return@runBlocking
            kotlinx.coroutines.delay(20)
        }
        throw AssertionError("still waiting")
    }

    @Test
    fun theSameAccount_keepsItsCache_acrossLaunches() = runBlocking {
        owner.id = "aria"
        fillCache()

        cleanup.onAccount("aria")

        assertEquals(1, parties())
        assertEquals(1, repository.observePendingCount().first())
    }

    @Test
    fun signingOut_clearsEverything_waitingEntriesToo() = runBlocking {
        auth.userFlow.value = AuthUser("aria", null, null)
        cleanup.start(scope)
        fillCache()
        assertEquals(1, parties())

        auth.userFlow.value = null

        waitFor { parties() == 0 }
        assertEquals(0, repository.observePendingCount().first())
        assertEquals(null, owner.id)
    }

    @Test
    fun anotherAccount_neverSeesThePreviousOnesParties() = runBlocking {
        owner.id = "aria"
        fillCache()

        // Bram signs in on this phone, even with no sign-out seen (between launches, say).
        cleanup.onAccount("bram")

        assertEquals(0, parties())
        assertEquals("bram", owner.id)
    }

    @Test
    fun theGamesBackup_hasNoDaroodData() {
        val json = JSONObject(DataExportImport.exportToJson(BackupPayload(null, null, null, emptyList())))

        val keys = json.keys().asSequence().toList()
        assertTrue(keys.toString(), keys.none { "darood" in it.lowercase() || "party" in it.lowercase() || "username" in it.lowercase() })
        assertTrue(BackupPayload::class.java.declaredFields.none { "darood" in it.name.lowercase() || "part" in it.name.lowercase() })
    }
}
