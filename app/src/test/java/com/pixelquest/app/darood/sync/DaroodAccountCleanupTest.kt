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
    fun signingOut_clearsStudyPartiesToo_butKeepsTheQuranAndItsBookmarks() = runBlocking {
        val study = com.pixelquest.app.testing.FakeStudyServer(server)
        val studyRepo = com.pixelquest.app.study.data.StudyRepositoryImpl(
            study, db.studyDao(), db.partyDao(), { server.me }, clock = { 1_000L }, timeZone = { java.time.ZoneId.of("UTC") }
        )
        val timer = com.pixelquest.app.study.data.MemoryStudyTimerStore()
        var studyCleared = 0
        val withStudy = DaroodAccountCleanup(auth, db.daroodDao(), owner, studyData = {
            timer.clear()
            studyCleared++
        })
        owner.id = "aria"
        // A study party with an item, a tick still waiting, and a timer running; the Quran on the phone, with a bookmark.
        val party = server.seedParty("Physics group", "aria", type = com.pixelquest.app.party.domain.PartyType.STUDY)
        val item = study.seedItem(party, "aria", "Lab report")
        repository.refreshParties()
        studyRepo.refresh(party)
        server.offline = true
        studyRepo.complete(item)
        server.offline = false
        timer.start(party, 1_000L)
        db.quranDao().insertSurahs(listOf(com.pixelquest.app.quran.data.local.QuranSurahEntity(1, "الفاتحة", "Al-Faatiha", "The Opening", "Meccan", 5, 7, 1, null)))
        db.quranBookmarkDao().insert(com.pixelquest.app.quran.data.local.QuranBookmarkEntity(1, 5, 1_000L))
        assertEquals(1, studyRepo.observeItems(party).first().size)

        withStudy.onAccount(null)

        assertTrue(studyRepo.observeItems(party).first().isEmpty())
        assertEquals(0, studyRepo.observePendingCount().first())
        assertTrue(studyRepo.observeCompletions(party).first().isEmpty())
        assertEquals(1, studyCleared)
        assertEquals(null, timer.running.value)
        // The Quran and the player's bookmarks are the phone's: they stay.
        assertEquals(1, db.quranDao().surahCount())
        assertEquals(1, db.quranBookmarkDao().count())
    }

    @Test
    fun theStudyStateKeptOutsideTheDatabase_goes_butNotificationChoicesStay() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("pixelquest_study", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("timer_party", "p1").putLong("timer_started_at", 5L).putString("winner_seen_p1", "2026-10-12")
            .putStringSet("due_reminders_sent", setOf("i:DAY")).putBoolean("due_reminders", false).putBoolean("weekly_summary", true).commit()
        val timer = com.pixelquest.app.study.data.PrefsStudyTimerStore(context)
        assertEquals("p1", timer.running.value?.partyId)

        com.pixelquest.app.study.data.PrefsStudyAccountData(context, timer).clear()

        assertEquals(null, timer.running.value)
        assertEquals(setOf("due_reminders", "weekly_summary"), prefs.all.keys)
        assertEquals(false, prefs.getBoolean("due_reminders", true))
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

    @Test
    fun aSignedInAccount_bringsItsInvitations_soTheMenuCanShowThem() = runBlocking {
        val family = server.seedParty("Family", "bram")
        server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(family, "aria", "member", "invited", null, null)
        val withRefresh = DaroodAccountCleanup(auth, db.daroodDao(), owner, repository)

        withRefresh.onAccount("aria")

        assertEquals(com.pixelquest.app.party.domain.MemberStatus.INVITED, repository.observeMyParties().first().single().myStatus)
    }
}
