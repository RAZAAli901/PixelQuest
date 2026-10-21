package com.pixelquest.app.study.ui

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The study party's screen logic, on a server and cache in memory: what each action does. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StudyPartyViewModelTest {

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = "aria").apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T10:00:00Z")
    }
    private val study = FakeStudyServer(server)
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { zone })
    private val repo = StudyRepositoryImpl(study, db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { zone })
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val viewModels = TestViewModels()
    private var watching: Job? = null

    @After
    fun tearDown() {
        watching?.cancel()
        viewModels.clear()
        db.closeAfterWrites()
    }

    private val timer = com.pixelquest.app.study.data.MemoryStudyTimerStore()

    private fun viewModel(): StudyPartyViewModel =
        viewModels.keep(StudyPartyViewModel::class.java) { StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, repo, null, timer) }
            .also { vm -> watching = vm.uiState.launchIn(MainScope()) }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            // Time passes on the main looper too, for what waits on it (the live refresh waits for a pause).
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(20))
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun addingAnItem_checksItsFields_thenSavesIt_inThePlayersZone() {
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded && vm.uiState.value.isLeader }

        vm.openAddItem()
        vm.saveForm()
        waitFor("the short title shown") { vm.uiState.value.form?.triedToSave == true }
        assertEquals(ItemFormProblem.TITLE_SHORT, vm.uiState.value.form?.problem)

        vm.updateForm(vm.uiState.value.form!!.copy(kind = StudyItemKind.QUIZ).withTitle("Waves quiz"))
        vm.saveForm()
        waitFor("the missing date shown") { vm.uiState.value.form?.problem == ItemFormProblem.QUIZ_NEEDS_DATE }
        assertTrue("nothing sent yet", study.calls.none { it == "addItem" })

        vm.updateForm(vm.uiState.value.form!!.copy(dueDay = LocalDate.of(2026, 10, 23), dueTime = LocalTime.of(9, 0), pinned = true))
        vm.saveForm()
        waitFor("the sheet closed") { vm.uiState.value.form == null }
        val saved = study.items.values.single()
        assertEquals(Instant.parse("2026-10-23T04:00:00Z"), saved.dueAt)
        assertTrue(saved.pinned)
        waitFor("on the screen") { vm.uiState.value.items.map { it.item.title } == listOf("Waves quiz") }
    }

    @Test
    fun editingAndDeleting_areForTheCreatorOrTheLeader() {
        val mine = study.seedItem(party, "bram", "Lab report")
        val arias = study.seedItem(party, "aria", "Waves quiz", StudyItemKind.QUIZ, dueAt = Instant.parse("2026-10-23T04:00:00Z"))
        server.me = "bram"
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.items.size == 2 }

        // Not his to change: nothing opens.
        vm.editItem(arias.id)
        vm.askDelete(arias.id)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(vm.uiState.value.form)
        assertNull(vm.uiState.value.confirmingDelete)

        // His own: the sheet opens with its fields, and saves.
        vm.editItem(mine.id)
        waitFor("the edit sheet") { vm.uiState.value.form?.editingId == mine.id }
        assertEquals("Lab report", vm.uiState.value.form?.title)
        vm.updateForm(vm.uiState.value.form!!.withTitle("Lab report, part 2"))
        vm.saveForm()
        waitFor("saved") { vm.uiState.value.form == null }
        assertEquals("Lab report, part 2", study.items.getValue(mine.id).title)

        // Deleting asks first, then goes.
        vm.askDelete(mine.id)
        waitFor("asked") { vm.uiState.value.confirmingDelete?.item?.id == mine.id }
        vm.confirmDelete()
        waitFor("gone") { vm.uiState.value.items.none { it.item.id == mine.id } }
        assertTrue(mine.id !in study.items)
    }

    @Test
    fun done_countsAtOnce_waitsOffline_andCanBeUndone() {
        val item = study.seedItem(party, "bram", "Lab report")
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.items.isNotEmpty() }

        server.offline = true
        vm.complete(item.id)
        waitFor("ticked on the phone") { vm.uiState.value.items.single().done }
        // The tick shows at once; the notice follows the send that couldn't go.
        waitFor("said offline") { vm.uiState.value.notice == StudyNotice.Offline }
        assertEquals(1, vm.uiState.value.items.single().doneCount)
        assertEquals(com.pixelquest.app.study.domain.SyncState.PENDING, vm.uiState.value.items.single().mine?.state)

        server.offline = false
        kotlinx.coroutines.runBlocking { repo.sendPending() }
        waitFor("sent") { vm.uiState.value.items.single().mine?.state == com.pixelquest.app.study.domain.SyncState.SYNCED }
        assertEquals(1, study.completions.size)

        vm.undo(item.id)
        waitFor("taken back") { !vm.uiState.value.items.single().done }
        assertTrue(study.completions.isEmpty())
    }

    @Test
    fun theTimer_logsItsMinutes_upTo240_andNothingUnder5() {
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded }

        vm.startTimer()
        waitFor("running") { vm.uiState.value.timer?.partyId == party }
        server.now = server.now.plusSeconds(4 * 60 + 59)
        vm.stopTimer()
        waitFor("too short") { vm.uiState.value.notice == StudyNotice.TooShort }
        assertNull(vm.uiState.value.timer)
        assertTrue(study.sessions.isEmpty())

        vm.startTimer()
        server.now = server.now.plusSeconds(50 * 60 + 30)
        vm.stopTimer()
        waitFor("50 minutes logged") { vm.uiState.value.studiedToday == 50 }
        vm.logStudy(25)
        waitFor("and 25 by hand") { vm.uiState.value.studiedToday == 75 }

        vm.startTimer()
        server.now = server.now.plusSeconds(5 * 3600)
        vm.stopTimer()
        waitFor("240 at most") { vm.uiState.value.studiedToday == 315 }
        waitFor("all sent") { study.sessions.size == 3 }
        assertEquals(listOf(50, 25, 240), study.sessions.map { it.minutes })
    }

    @Test
    fun aTimerRunningElsewhere_isntThisPartysToStop() {
        timer.start("another-party", server.now.toEpochMilli())
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded }
        vm.startTimer()
        vm.stopTimer()
        vm.cancelTimer()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("another-party", timer.running.value?.partyId)
    }

    @Test
    fun lastWeeksWinner_isAnnounced_untilDismissed() {
        val item = study.seedItem(party, "aria", "Lab report")
        study.seedCompletion(item.id, "bram", LocalDate.of(2026, 10, 21))
        server.now = Instant.parse("2026-10-27T10:00:00Z")
        val seen = com.pixelquest.app.study.data.MemoryWinnerSeenStore()
        val vm = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, repo, null, timer, seen)
        }.also { watching = it.uiState.launchIn(MainScope()) }

        waitFor("the banner") { vm.uiState.value.winnerBanner.map { it.userId } == listOf("bram") }
        assertEquals(LocalDate.of(2026, 10, 19), vm.uiState.value.pastWeeks.single().weekStart)
        vm.dismissWinnerBanner()
        waitFor("dismissed") { vm.uiState.value.winnerBanner.isEmpty() }
        assertEquals(LocalDate.of(2026, 10, 19), seen.seenWeek(party))
    }

    @Test
    fun inSimpleMode_thereIsNoScoreboard_norWinnerBanner() {
        val item = study.seedItem(party, "aria", "Lab report")
        study.seedCompletion(item.id, "bram", LocalDate.of(2026, 10, 21))
        server.now = Instant.parse("2026-10-27T10:00:00Z")
        val simple = io.mockk.mockk<com.pixelquest.app.domain.repository.SettingsRepository>(relaxed = true) {
            io.mockk.every { simpleModeEnabled } returns kotlinx.coroutines.flow.flowOf(true)
        }
        val vm = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, repo, simple, timer)
        }.also { watching = it.uiState.launchIn(MainScope()) }
        waitFor("loaded, simply") { vm.uiState.value.loaded && vm.uiState.value.simpleMode }
        assertEquals(listOf(com.pixelquest.app.party.domain.PartyTab.THIS_WEEK, com.pixelquest.app.party.domain.PartyTab.ALL_ITEMS, com.pixelquest.app.party.domain.PartyTab.PEOPLE), vm.uiState.value.tabs)
        vm.selectTab(com.pixelquest.app.party.domain.PartyTab.SCOREBOARD)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(com.pixelquest.app.party.domain.PartyTab.THIS_WEEK, vm.uiState.value.tab)
        waitFor("last week recorded") { vm.uiState.value.pastWeeks.isNotEmpty() }
        assertTrue("no winner banner", vm.uiState.value.winnerBanner.isEmpty())
    }

    @Test
    fun offline_itShowsWhatThePhoneHas_andSaysSo() {
        val item = study.seedItem(party, "bram", "Lab report")
        val first = viewModel()
        waitFor("cached") { first.uiState.value.items.isNotEmpty() }
        watching?.cancel()
        viewModels.clear()

        server.offline = true
        val vm = viewModel()
        waitFor("loaded from the phone") { vm.uiState.value.loaded }
        assertEquals(listOf(item.id), vm.uiState.value.items.map { it.item.id })
        assertEquals(StudyNotice.Offline, vm.uiState.value.notice)
        vm.dismissNotice()
        waitFor("dismissed") { vm.uiState.value.notice == null }
    }

    @Test
    fun aRefusedTick_canBeTriedAgain_orDropped_fromTheScreen() {
        val item = study.seedItem(party, "bram", "Lab report")
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.items.isNotEmpty() }
        study.items.remove(item.id)

        vm.complete(item.id)
        waitFor("refused") { vm.uiState.value.refused.isNotEmpty() }
        val key = vm.uiState.value.refused.single().clientId!!
        vm.retry(key)
        waitFor("refused again") { study.calls.count { it == "complete" } == 2 && vm.uiState.value.refused.size == 1 }
        vm.discard(key)
        waitFor("dropped") { vm.uiState.value.refused.isEmpty() && vm.uiState.value.items.single().mine == null }
    }

    @Test
    fun studyTime_outsideFiveTo240_isntLogged() {
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded }
        vm.logStudy(4)
        vm.logStudy(241)
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)
        assertTrue(study.sessions.isEmpty())
        assertEquals(0, vm.uiState.value.studiedToday)
    }

    @Test
    fun aChangeOfMembers_heardLive_refreshesThem() {
        val changes = kotlinx.coroutines.channels.Channel<com.pixelquest.app.study.sync.StudyChange>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val live = com.pixelquest.app.study.sync.StudyLiveSource { kotlinx.coroutines.flow.flow { for (c in changes) emit(c) } }
        val following = StudyRepositoryImpl(study, db.studyDao(), db.partyDao(), { server.me }, clock = { server.now.toEpochMilli() }, timeZone = { zone }, live = live, quietMs = 50)
        val vm = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, following, null, timer)
        }.also { watching = it.uiState.launchIn(MainScope()) }
        waitFor("loaded") { vm.uiState.value.memberCount == 2 }
        val follow = MainScope().launch { vm.followWhileVisible() }

        server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(party, "cleo", "member", "active", server.now.toString(), server.now.toString())
        changes.trySend(com.pixelquest.app.study.sync.StudyChange.MembersChanged)
        waitFor("Cleo on the screen") { vm.uiState.value.memberCount == 3 }

        study.seedItem(party, "cleo", "Essay")
        changes.trySend(com.pixelquest.app.study.sync.StudyChange.Activity)
        waitFor("her item too") { vm.uiState.value.items.map { it.item.title } == listOf("Essay") }
        follow.cancel()
    }

    @Test
    fun nothingHere_touchesXpLevelsOrStreaks() {
        kotlinx.coroutines.runBlocking {
            db.userProfileDao().insertProfile(com.pixelquest.app.data.local.entity.UserProfileEntity(username = "Aria", avatarId = "a1", level = 4, totalXp = 1500))
        }
        val item = study.seedItem(party, "bram", "Lab report")
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.items.isNotEmpty() }
        vm.complete(item.id)
        vm.logStudy(60)
        waitFor("sent") { study.completions.size == 1 && study.sessions.size == 1 }
        kotlinx.coroutines.runBlocking {
            val profile = db.userProfileDao().getProfile().first()!!
            assertEquals(4, profile.level)
            assertEquals(1500, profile.totalXp)
            assertNull(db.streakDao().getCurrentStreak().first())
            assertTrue(db.taskCompletionLogDao().getAllLogs().first().isEmpty())
        }
    }

    @Test
    fun theLeader_pinsFromTheEditSheet() {
        val item = study.seedItem(party, "bram", "Lab report")
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.items.isNotEmpty() && vm.uiState.value.isLeader }
        vm.editItem(item.id)
        waitFor("the edit sheet") { vm.uiState.value.form != null }
        vm.updateForm(vm.uiState.value.form!!.copy(pinned = true))
        vm.saveForm()
        waitFor("pinned") { vm.uiState.value.pinned.map { it.item.id } == listOf(item.id) }
        assertTrue(study.items.getValue(item.id).pinned)
    }

    @Test
    fun aMember_cantPin_andARefusalKeepsTheSheetOpen() {
        server.me = "bram"
        val vm = viewModel()
        waitFor("loaded") { vm.uiState.value.loaded }

        vm.openAddItem()
        vm.updateForm(vm.uiState.value.form!!.withTitle("Lab report").copy(pinned = true))
        vm.saveForm()
        waitFor("the sheet closed") { vm.uiState.value.form == null }
        assertEquals("only the leader pins", false, study.items.values.single().pinned)

        server.offline = true
        vm.openAddItem()
        vm.updateForm(vm.uiState.value.form!!.withTitle("Essay"))
        vm.saveForm()
        waitFor("told it's offline") { vm.uiState.value.notice == StudyNotice.Offline }
        assertEquals("Essay", vm.uiState.value.form?.title)
        assertEquals(false, vm.uiState.value.form?.saving)
        vm.closeForm()
        waitFor("closed") { vm.uiState.value.form == null }
        assertNull(vm.uiState.value.form)
    }
}
