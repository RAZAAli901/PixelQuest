package com.pixelquest.app.study.ui

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.MemoryStudyTimerStore
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.data.remote.StudyRemoteDataSource
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * F02: Verifies that when User A taps Done on a study circle item, the item
 * disappears from User A's active list, while remaining visible for User B.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyCircleDoneItemTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T05:00:00Z") // Wednesday in week of 2026-10-19
    }
    private val study = FakeStudyServer(server)
    private val party = server.seedParty("Math Study Group", "aria", "bram", type = PartyType.STUDY)
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = TestViewModels()

    @After
    fun tearDown() {
        viewModels.clear()
        databases.forEach { it.closeAfterWrites() }
    }

    private inline fun <reified T> asPlayer(userId: String, target: T): T = java.lang.reflect.Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, args ->
        synchronized(server) {
            server.me = userId
            method.invoke(target, *(args ?: emptyArray()))
        }
    } as T

    /** Represents a player's device with its own local Room database and ViewModel. */
    private inner class Phone(
        userId: String,
        liveSource: com.pixelquest.app.study.sync.StudyLiveSource? = null,
        quietMs: Long = 1_000L,
    ) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val parties = DaroodPartyRepositoryImpl(
            asPlayer<DaroodPartyRemoteDataSource>(userId, server),
            db.daroodDao(),
            { userId },
            now = { server.now.toEpochMilli() },
            zone = { zone },
        )
        val repo = StudyRepositoryImpl(
            asPlayer<StudyRemoteDataSource>(userId, study),
            db.studyDao(),
            db.partyDao(),
            { userId },
            clock = { server.now.toEpochMilli() },
            timeZone = { zone },
            live = liveSource,
            quietMs = quietMs,
        )
        val vm: StudyPartyViewModel = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(
                SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)),
                parties,
                repo,
                null,
                MemoryStudyTimerStore(),
            )
        }.also { it.uiState.launchIn(kotlinx.coroutines.MainScope()) }
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun userATapsDone_itemDisappearsForUserA_andStillShowsForUserB() {
        // Seed a quiz item due this Friday
        val quiz = study.seedItem(
            party = party,
            by = "aria",
            title = "Calculus Quiz",
            kind = StudyItemKind.QUIZ,
            dueAt = Instant.parse("2026-10-23T04:00:00Z"),
        )

        val aria = Phone("aria")
        val bram = Phone("bram")

        // Wait for both to load the party and observe the item in This week
        waitFor("Aria loaded") { aria.vm.uiState.value.loaded && aria.vm.uiState.value.thisWeek.any { it.item.id == quiz.id } }
        waitFor("Bram loaded") { bram.vm.uiState.value.loaded && bram.vm.uiState.value.thisWeek.any { it.item.id == quiz.id } }

        // Both see 0 of 2 members done initially
        val ariaItemBefore = aria.vm.uiState.value.thisWeek.first { it.item.id == quiz.id }
        val bramItemBefore = bram.vm.uiState.value.thisWeek.first { it.item.id == quiz.id }
        assertEquals(0, ariaItemBefore.doneCount)
        assertEquals(0, bramItemBefore.doneCount)
        assertFalse(ariaItemBefore.done)
        assertFalse(bramItemBefore.done)

        // User A (Aria) taps Done
        aria.vm.complete(quiz.id)

        // Aria's completion is recorded and synced to server
        waitFor("Aria completion on server") { study.completions.any { it.itemId == quiz.id && it.userId == "aria" } }

        // Bram's phone refreshes from server
        bram.vm.refresh()
        waitFor("Bram refreshed") {
            bram.vm.uiState.value.loaded &&
                bram.vm.uiState.value.items.firstOrNull { it.item.id == quiz.id }?.doneCount == 1
        }

        // Assertion 1: Item MUST disappear from User A's active This week list
        assertFalse(
            "Completed item must disappear from User A's active thisWeek list",
            aria.vm.uiState.value.thisWeek.any { it.item.id == quiz.id },
        )

        // Assertion 2: Item MUST still show in User B's active This week list
        assertTrue(
            "Incomplete item must still show in User B's active thisWeek list",
            bram.vm.uiState.value.thisWeek.any { it.item.id == quiz.id },
        )

        // Assertion 3: User B sees User A's completion reflected in the done count
        val bramItemAfter = bram.vm.uiState.value.thisWeek.first { it.item.id == quiz.id }
        assertEquals(1, bramItemAfter.doneCount)
        assertEquals(2, bramItemAfter.memberCount)
        assertFalse("Item must still not be marked done for User B", bramItemAfter.done)
    }

    @Test
    fun twoUsers_undo_offlineCompletionSyncsLater_andRealtimeUpdate() {
        val quiz = study.seedItem(
            party = party,
            by = "aria",
            title = "Mechanics Quiz",
            kind = StudyItemKind.QUIZ,
            dueAt = Instant.parse("2026-10-23T04:00:00Z"),
        )

        val aria = Phone("aria")

        // Bram's device has live updates configured
        val bramChanges = kotlinx.coroutines.channels.Channel<com.pixelquest.app.study.sync.StudyChange>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val bramLive = com.pixelquest.app.study.sync.StudyLiveSource { kotlinx.coroutines.flow.flow { for (c in bramChanges) emit(c) } }
        val bram = Phone("bram", liveSource = bramLive, quietMs = 50)

        // Wait for both to load
        waitFor("Aria loaded") { aria.vm.uiState.value.loaded && aria.vm.uiState.value.thisWeek.any { it.item.id == quiz.id } }
        waitFor("Bram loaded") { bram.vm.uiState.value.loaded && bram.vm.uiState.value.thisWeek.any { it.item.id == quiz.id } }

        val bramFollowJob = kotlinx.coroutines.MainScope().launch { bram.vm.followWhileVisible() }

        // 1. Offline completion: Server goes offline
        server.offline = true
        aria.vm.complete(quiz.id)

        // Local cache updates at once: item is removed from active thisWeek and moves to completedThisWeek
        waitFor("Aria offline completion recorded") {
            aria.vm.uiState.value.completedThisWeek.any { it.item.id == quiz.id } &&
                aria.vm.uiState.value.thisWeek.none { it.item.id == quiz.id }
        }
        val ariaCompletedRow = aria.vm.uiState.value.completedThisWeek.first { it.item.id == quiz.id }
        assertEquals(com.pixelquest.app.study.domain.SyncState.PENDING, ariaCompletedRow.mine?.state)

        // 2. Server comes back online and syncs pending completion
        server.offline = false
        kotlinx.coroutines.runBlocking { aria.repo.sendPending() }
        waitFor("Synced to server") {
            study.completions.any { it.itemId == quiz.id && it.userId == "aria" }
        }

        // 3. Realtime update arriving while Bram's screen is open
        bramChanges.trySend(com.pixelquest.app.study.sync.StudyChange.Activity)
        waitFor("Bram receives realtime update") {
            val bramRow = bram.vm.uiState.value.thisWeek.firstOrNull { it.item.id == quiz.id }
            bramRow != null && bramRow.doneCount == 1 && "aria_k" in bramRow.doneUsers
        }
        // Item still shows for Bram because Bram has not completed it
        assertTrue(bram.vm.uiState.value.thisWeek.any { it.item.id == quiz.id })

        // 4. Undo: User A (Aria) takes the completion back within 24h
        aria.vm.undo(quiz.id)
        waitFor("Undo reflected on Aria phone") {
            aria.vm.uiState.value.thisWeek.any { it.item.id == quiz.id } &&
                aria.vm.uiState.value.completedThisWeek.none { it.item.id == quiz.id }
        }
        waitFor("Undo reflected on server") {
            study.completions.none { it.itemId == quiz.id && it.userId == "aria" }
        }

        // Realtime update of undo arrives on Bram's open screen
        bramChanges.trySend(com.pixelquest.app.study.sync.StudyChange.Activity)
        waitFor("Bram receives realtime update after undo") {
            val bramRow = bram.vm.uiState.value.thisWeek.firstOrNull { it.item.id == quiz.id }
            bramRow != null && bramRow.doneCount == 0 && bramRow.doneUsers.isEmpty()
        }

        bramFollowJob.cancel()
    }
}
