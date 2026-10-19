package com.pixelquest.app.darood.domain

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.ui.chat.PartyChatViewModel
import com.pixelquest.app.darood.ui.chat.myStatsOf
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The player's numbers on a fixed clock, in different time zones: the day is always the poster's own
 * day where they were, and "today" is the phone's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodStatsClockTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    private val karachi = ZoneId.of("Asia/Karachi")
    private val losAngeles = ZoneId.of("America/Los_Angeles")
    private val utc = ZoneId.of("UTC")

    /** 20:00 in London-winter time on the 19th: already the 20th in Karachi, still the 19th in Los Angeles. */
    private val evening = Instant.parse("2026-10-19T20:00:00Z")

    private val server = FakeDaroodServer(me = "aria-id").apply { usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram") }

    private fun repository(at: Instant, zone: ZoneId) =
        DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { at.toEpochMilli() }, zone = { zone })

    @Test
    fun today_isThePhonesDay_whereItIs() {
        assertEquals(LocalDate.of(2026, 10, 20), repository(evening, karachi).today())
        assertEquals(LocalDate.of(2026, 10, 19), repository(evening, utc).today())
        assertEquals(LocalDate.of(2026, 10, 19), repository(evening, losAngeles).today())
    }

    @Test
    fun anEntryLateAtNight_countsForTheDayItWasPostedOn_whereItWasPosted() = runBlocking {
        val partyId = server.seedParty("Friday circle", "aria-id")
        // 23:30 in Karachi on the 19th is 18:30 UTC: the 19th there, whatever the server's clock says.
        val lateInKarachi = Instant.parse("2026-10-19T18:30:00Z")
        val phone = repository(lateInKarachi, karachi)
        phone.refreshParty(partyId)
        phone.addEntry(partyId, 313)

        val days = (phone.fetchMyDays(partyId) as SupabaseResult.Success).data
        assertEquals(mapOf(LocalDate.of(2026, 10, 19) to 313L), days)
        val steady = Consistency.of(days, phone.today())
        assertEquals(1, steady.currentStreak)
        assertEquals(1, steady.activeLast30)
    }

    @Test
    fun aStreak_survivesTravel_dayByLocalDay() = runBlocking {
        val partyId = server.seedParty("Friday circle", "aria-id")
        // The 17th at home in London, the 18th after landing in Karachi, the 19th in Los Angeles: each
        // posted in the evening, local time.
        for ((at, zone) in listOf(
            Instant.parse("2026-10-17T19:00:00Z") to utc,
            Instant.parse("2026-10-18T15:00:00Z") to karachi,
            Instant.parse("2026-10-20T02:00:00Z") to losAngeles
        )) {
            val phone = repository(at, zone)
            phone.refreshParty(partyId)
            phone.addEntry(partyId, 100)
        }
        val days = (repository(evening, losAngeles).fetchMyDays(partyId) as SupabaseResult.Success).data
        assertEquals(setOf(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19)), days.keys)

        // In Los Angeles that evening it's still the 19th: three in a row.
        assertEquals(3, Consistency.of(days, LocalDate.of(2026, 10, 19)).currentStreak)
        // The next day, before posting: still three, today not being over.
        assertEquals(3, Consistency.of(days, LocalDate.of(2026, 10, 20)).currentStreak)
        // A day missed: over, but the best stays.
        val later = Consistency.of(days, LocalDate.of(2026, 10, 21))
        assertEquals(0, later.currentStreak)
        assertEquals(3, later.bestStreak)
        assertEquals(100, later.averagePerActiveDay)
    }

    @Test
    fun theRank_inEveryShapeOfParty() {
        fun stat(user: String, total: Long) = MemberStats("p1", user, user, total, 1, null)
        // Everyone level: all first.
        val level = listOf(stat("a", 100), stat("b", 100), stat("c", 100))
        assertEquals(listOf(1, 1, 1), level.map { myStatsOf(level, 300, it.userId).place })
        // Alone: first of one, all of it.
        assertEquals(MyStatsCheck(place = 1, posters = 1, share = 1.0), check(myStatsOf(listOf(stat("a", 50)), 50, "a")))
        // Others have posted, not me: no place, no share yet.
        assertEquals(MyStatsCheck(place = null, posters = 2, share = 0.0), check(myStatsOf(listOf(stat("a", 50), stat("b", 20)), 70, "me")))
        // People who left still count toward the places and the total.
        val withLeaver = listOf(stat("left-early", 900), stat("me", 100))
        assertEquals(MyStatsCheck(place = 2, posters = 2, share = 0.1), check(myStatsOf(withLeaver, 1_000, "me")))
    }

    private data class MyStatsCheck(val place: Int?, val posters: Int, val share: Double?)

    private fun check(stats: com.pixelquest.app.darood.ui.chat.MyStats) = MyStatsCheck(stats.place, stats.posters, stats.share)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun theChat_countsConsistency_onThePhonesClock() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = ViewModelStore()
        val collectors = CoroutineScope(Dispatchers.Unconfined)
        try {
            val partyId = server.seedParty("Friday circle", "aria-id", "bram-id")
            for (back in listOf(0L, 1L, 2L, 5L)) server.seedEntry(partyId, "aria-id", 100 * (back + 1).toInt(), day = LocalDate.of(2026, 10, 20).minusDays(back))
            server.seedEntry(partyId, "bram-id", 999, day = LocalDate.of(2026, 10, 20))
            val phone = repository(evening, karachi)
            runBlocking { phone.refreshParty(partyId) }
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PartyChatViewModel(SavedStateHandle(mapOf("partyId" to partyId)), phone, { "aria-id" }) as T
            }
            val vm = ViewModelProvider(store, factory)[PartyChatViewModel::class.java]
            collectors.launch { vm.consistency.collect { } }
            vm.refreshConsistency()
            run {
                repeat(150) {
                    if (vm.consistency.value != null) return@run
                    Thread.sleep(20)
                }
            }
            val steady = vm.consistency.value!!
            assertEquals("today the 20th in Karachi", LocalDate.of(2026, 10, 20), steady.asOf)
            assertEquals(3, steady.currentStreak)
            assertEquals(4, steady.activeLast30)
            assertEquals(3, steady.bestStreak)
            assertEquals("only Aria's own days", (100 + 200 + 300 + 600) / 4L, steady.averagePerActiveDay)
        } finally {
            collectors.cancel()
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
