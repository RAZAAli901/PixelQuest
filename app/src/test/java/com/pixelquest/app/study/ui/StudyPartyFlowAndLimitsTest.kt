package com.pixelquest.app.study.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * V045: Study party flow and limits suite.
 *
 * User flow:
 * - Pin an item, complete it, verify it hides for the completer while remaining active for others.
 *
 * Limits and edge cases:
 * - Due date edge cases: overdue past dates, far-future dates (2099+), undated items.
 * - Concurrency: two members completing the same item simultaneously without collision or duplicate counts.
 * - Undo window: undo allowed within 24 hours of completion; rejected after 24 hours.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyPartyFlowAndLimitsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val zone = ZoneId.of("Asia/Karachi")
    private val initialTime = Instant.parse("2026-10-21T05:00:00Z")
    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = initialTime
    }
    private val studyServer = FakeStudyServer(server)
    private val partyId = server.seedParty("Physics Study Group", "aria", "bram", type = PartyType.STUDY)
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = TestViewModels()
    private val collectors = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        collectors.cancel()
        viewModels.clear()
        Dispatchers.resetMain()
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

    private inner class TestPhone(val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }

        val partyRepo = DaroodPartyRepositoryImpl(
            asPlayer<DaroodPartyRemoteDataSource>(userId, server),
            db.daroodDao(),
            { userId },
            now = { server.now.toEpochMilli() },
            zone = { zone },
        )

        val studyRepo = StudyRepositoryImpl(
            asPlayer<StudyRemoteDataSource>(userId, studyServer),
            db.studyDao(),
            db.partyDao(),
            { userId },
            clock = { server.now.toEpochMilli() },
            timeZone = { zone },
        )

        val vm: StudyPartyViewModel = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(
                SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to partyId)),
                partyRepo,
                studyRepo,
                null,
                MemoryStudyTimerStore(),
            )
        }.also { vm ->
            collectors.launch { vm.uiState.collect {} }
            vm.refresh()
        }
    }

    private fun waitFor(desc: String, condition: () -> Boolean) = runBlocking {
        repeat(150) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (condition()) return@runBlocking
            delay(20)
        }
        throw AssertionError("Condition not met: $desc")
    }

    @Test
    fun userFlow_pinItem_completeIt_and_seeItHideForMeOnly() {
        val aria = TestPhone("aria")
        val bram = TestPhone("bram")

        waitFor("Aria party loaded") { aria.vm.uiState.value.loaded && aria.vm.uiState.value.isLeader }
        waitFor("Bram party loaded") { bram.vm.uiState.value.loaded }

        // 1. Leader Aria adds a pinned quiz due Friday
        val quiz = studyServer.seedItem(
            party = partyId,
            by = "aria",
            title = "Waves Quiz",
            kind = StudyItemKind.QUIZ,
            dueAt = initialTime.plus(Duration.ofDays(2)),
            pinned = true,
        )

        aria.vm.refresh()
        bram.vm.refresh()

        waitFor("Quiz visible on Aria's screen") {
            aria.vm.uiState.value.items.any { it.item.id == quiz.id }
        }
        waitFor("Quiz visible on Bram's screen") {
            bram.vm.uiState.value.items.any { it.item.id == quiz.id }
        }

        // Both Aria and Bram see it as active in This Week initially
        val bramRowBefore = bram.vm.uiState.value.items.first { it.item.id == quiz.id }
        assertFalse("Bram has not completed it yet", bramRowBefore.done)
        assertTrue("Quiz is in Bram's This Week", bram.vm.uiState.value.thisWeek.any { it.item.id == quiz.id })
        assertTrue("Quiz is in Aria's This Week", aria.vm.uiState.value.thisWeek.any { it.item.id == quiz.id })

        // 2. Bram completes the quiz
        bram.vm.complete(quiz.id)

        waitFor("Completion recorded on server") {
            studyServer.completions.any { it.itemId == quiz.id && it.userId == "bram" }
        }

        bram.vm.refresh()
        aria.vm.refresh()

        // 3. Verify it hides for Bram while remaining visible for Aria:
        // On Bram's screen: it is completed, so it is hidden from This Week / active items
        waitFor("Bram's UI reflects completion") {
            bram.vm.uiState.value.items.firstOrNull { it.item.id == quiz.id }?.done == true
        }
        assertFalse("Quiz is hidden from Bram's active This Week", bram.vm.uiState.value.thisWeek.any { it.item.id == quiz.id })
        assertTrue("Quiz is in Bram's completed items", bram.vm.uiState.value.completedItems.any { it.item.id == quiz.id })

        // On Aria's screen: Aria has NOT completed it, and not everyone has finished (1/2 members)
        waitFor("Aria's UI reflects Bram's completion") {
            aria.vm.uiState.value.items.firstOrNull { it.item.id == quiz.id }?.doneCount == 1
        }
        val ariaRowAfter = aria.vm.uiState.value.items.first { it.item.id == quiz.id }
        assertFalse("Quiz is NOT done by Aria", ariaRowAfter.done)
        assertEquals("Done count is 1", 1, ariaRowAfter.doneCount)
        assertFalse("Not everyone is done", ariaRowAfter.everyoneDone)
        assertTrue("Quiz is STILL in Aria's This Week", aria.vm.uiState.value.thisWeek.any { it.item.id == quiz.id })
    }

    @Test
    fun dueDates_pastOverdue_and_farFutureDates() {
        val now = initialTime

        // 1. Past due date (3 days ago)
        val pastDue = now.minus(Duration.ofDays(3))
        val overdueState = dueStateOf(pastDue, now, zone)
        assertEquals("Past due date must be OVERDUE", DueState.OVERDUE, overdueState)

        // 2. Due today
        val todayDue = now.plus(Duration.ofHours(3))
        val todayState = dueStateOf(todayDue, now, zone)
        assertEquals("Due today must be TODAY", DueState.TODAY, todayState)

        // 3. Due soon (tomorrow)
        val soonDue = now.plus(Duration.ofDays(1)).plus(Duration.ofHours(5))
        val soonState = dueStateOf(soonDue, now, zone)
        assertEquals("Due tomorrow must be SOON", DueState.SOON, soonState)

        // 4. Far future date (Year 2099)
        val farFutureDue = ZonedDateTime.of(2099, 12, 31, 23, 59, 59, 0, zone).toInstant()
        val farFutureState = dueStateOf(farFutureDue, now, zone)
        assertEquals("Far future date must be LATER", DueState.LATER, farFutureState)

        // 5. Undated item
        val noneState = dueStateOf(null, now, zone)
        assertEquals("Null due date must be NONE", DueState.NONE, noneState)
    }

    @Test
    fun twoUsers_completingAtOnce_concurrentCompletionsHandledCleanly() {
        val aria = TestPhone("aria")
        val bram = TestPhone("bram")

        val lab = studyServer.seedItem(party = partyId, by = "aria", title = "Chemistry Lab")

        aria.vm.refresh()
        bram.vm.refresh()

        waitFor("Lab visible") {
            aria.vm.uiState.value.items.any { it.item.id == lab.id } &&
                bram.vm.uiState.value.items.any { it.item.id == lab.id }
        }

        // Concurrently complete the item from both phones
        aria.vm.complete(lab.id)
        bram.vm.complete(lab.id)

        waitFor("Both completions registered") {
            studyServer.completions.count { it.itemId == lab.id } == 2
        }

        aria.vm.refresh()
        bram.vm.refresh()

        waitFor("Both phones see everyone done") {
            val aRow = aria.vm.uiState.value.items.first { it.item.id == lab.id }
            val bRow = bram.vm.uiState.value.items.first { it.item.id == lab.id }
            aRow.everyoneDone && bRow.everyoneDone && aRow.doneCount == 2 && bRow.doneCount == 2
        }
    }

    @Test
    fun undo_within24HoursAllowed_after24HoursRejected() {
        val bram = TestPhone("bram")
        val assignment = studyServer.seedItem(party = partyId, by = "aria", title = "Math Homework")

        bram.vm.refresh()
        waitFor("Assignment loaded") { bram.vm.uiState.value.items.any { it.item.id == assignment.id } }

        // 1. Bram completes the item at t0
        bram.vm.complete(assignment.id)
        waitFor("Completed on server") { studyServer.completions.any { it.itemId == assignment.id && it.userId == "bram" } }

        bram.vm.refresh()
        waitFor("Bram sees marked done") { bram.vm.uiState.value.items.firstOrNull { it.item.id == assignment.id }?.done == true }
        val rowDone = bram.vm.uiState.value.items.first { it.item.id == assignment.id }
        assertTrue("Item is marked done", rowDone.done)
        assertTrue("Undo allowed within window", rowDone.canUndo)

        // 2. Undo within 24 hours: success
        bram.vm.undo(assignment.id)
        waitFor("Undo completed on server") { studyServer.completions.none { it.itemId == assignment.id && it.userId == "bram" } }

        bram.vm.refresh()
        waitFor("Bram sees undone") { bram.vm.uiState.value.items.firstOrNull { it.item.id == assignment.id }?.done == false }
        val rowUndone = bram.vm.uiState.value.items.first { it.item.id == assignment.id }
        assertFalse("Item is no longer marked done", rowUndone.done)

        // 3. Bram completes again
        bram.vm.complete(assignment.id)
        waitFor("Completed second time") { studyServer.completions.any { it.itemId == assignment.id && it.userId == "bram" } }
        bram.vm.refresh()
        waitFor("Bram sees completed second time") { bram.vm.uiState.value.items.firstOrNull { it.item.id == assignment.id }?.done == true }

        // 4. Advance time past 24-hour undo window (25 hours later)
        server.now = initialTime.plus(Duration.ofHours(25))
        bram.vm.refresh()
        waitFor("Bram's UI reflects expired undo window") {
            val r = bram.vm.uiState.value.items.firstOrNull { it.item.id == assignment.id }
            r != null && r.done && !r.canUndo
        }

        val rowExpired = bram.vm.uiState.value.items.first { it.item.id == assignment.id }
        assertTrue("Item is done", rowExpired.done)
        assertFalse("Undo is NOT allowed after 24h window", rowExpired.canUndo)

        // Calling undo directly on repository returns NOT_ALLOWED refusal
        val undoResult = runBlocking { bram.studyRepo.undo(assignment.id) }
        assertTrue("Undo after 24h is refused", undoResult is com.pixelquest.app.data.remote.SupabaseResult.ServerError)
        val refusal = StudyRefusal.of(undoResult)
        assertEquals(StudyRefusal.NOT_ALLOWED, refusal)

        // Completion remains on server
        assertEquals(1, studyServer.completions.count { it.itemId == assignment.id && it.userId == "bram" })
    }
}
