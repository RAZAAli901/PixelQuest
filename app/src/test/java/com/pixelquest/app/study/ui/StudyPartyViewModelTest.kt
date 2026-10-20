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
import kotlinx.coroutines.flow.launchIn
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
    private val repo = StudyRepositoryImpl(study, db.studyDao(), db.partyDao(), { server.me }, { server.now.toEpochMilli() }, { zone })
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val viewModels = TestViewModels()
    private var watching: Job? = null

    @After
    fun tearDown() {
        watching?.cancel()
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun viewModel(): StudyPartyViewModel =
        viewModels.keep(StudyPartyViewModel::class.java) { StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, repo) }
            .also { vm -> watching = vm.uiState.launchIn(MainScope()) }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
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
