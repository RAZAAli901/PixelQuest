package com.pixelquest.app.darood.ui.list

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.PartyMemberDto
import com.pixelquest.app.testing.FakeDaroodServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The party list: the account's parties and invitations, kept current, and answering invitations. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodPartiesViewModelTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "me").apply { usernames += mapOf("me" to "aria_k", "u2" to "bram") }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })
    private val auth = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun viewModel() = DaroodPartiesViewModel(auth, repository).also { vm ->
        // The screen collects it; so does the test.
        kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined).launch { vm.uiState.collect { } }
    }

    private fun waitFor(condition: () -> Boolean) = runBlocking {
        repeat(100) {
            if (condition()) return@runBlocking
            kotlinx.coroutines.delay(20)
        }
        throw AssertionError("still waiting")
    }

    @Test
    fun signedIn_theListComesFromTheServer_partiesAndInvitesApart() {
        server.seedParty("Friday circle", "me", "u2")
        val family = server.seedParty("Family", "u2")
        server.members += PartyMemberDto(family, "me", "member", "invited", null, null)
        val vm = viewModel()

        auth.userFlow.value = AuthUser("me", null, null)
        waitFor { vm.uiState.value.parties.size == 1 }

        val state = vm.uiState.value
        assertEquals(true, state.signedIn)
        assertEquals(listOf("Friday circle"), state.parties.map { it.name })
        assertEquals(listOf("Family"), state.invites.map { it.name })
        assertNull(state.problem)
    }

    @Test
    fun signedOut_thereIsNothingToList() {
        server.seedParty("Friday circle", "me")
        val vm = viewModel()
        auth.userFlow.value = null
        waitFor { vm.uiState.value.signedIn == false }
        assertEquals(true, vm.uiState.value.isEmpty)
    }

    @Test
    fun acceptingAnInvitation_movesItIntoTheParties() {
        val family = server.seedParty("Family", "u2")
        server.members += PartyMemberDto(family, "me", "member", "invited", null, null)
        auth.userFlow.value = AuthUser("me", null, null)
        val vm = viewModel()
        waitFor { vm.uiState.value.invites.size == 1 }

        vm.accept(family)
        waitFor { vm.uiState.value.parties.size == 1 && vm.uiState.value.answering == null }

        assertEquals(emptyList<Any>(), vm.uiState.value.invites)
        assertNull(vm.uiState.value.answering)
    }

    @Test
    fun decliningAnInvitation_dropsIt_andAWithdrawnOneSaysSo() {
        val family = server.seedParty("Family", "u2")
        val friends = server.seedParty("Friends", "u2")
        server.members += PartyMemberDto(family, "me", "member", "invited", null, null)
        server.members += PartyMemberDto(friends, "me", "member", "invited", null, null)
        auth.userFlow.value = AuthUser("me", null, null)
        val vm = viewModel()
        waitFor { vm.uiState.value.invites.size == 2 }

        vm.decline(family)
        waitFor { vm.uiState.value.invites.size == 1 && vm.uiState.value.answering == null }
        assertEquals(emptyList<Any>(), vm.uiState.value.parties)

        // The leader withdrew Friends meanwhile.
        server.members.replaceAll { if (it.partyId == friends) it.copy(status = "removed") else it }
        vm.accept(friends)
        waitFor { vm.uiState.value.answering == null && vm.uiState.value.invites.isEmpty() }
        assertEquals(PartiesProblem.INVITE_GONE, vm.uiState.value.problem)
    }

    @Test
    fun offline_theListStays_andSaysSo() {
        server.seedParty("Friday circle", "me")
        auth.userFlow.value = AuthUser("me", null, null)
        val vm = viewModel()
        waitFor { vm.uiState.value.parties.size == 1 }

        server.offline = true
        vm.refresh()
        waitFor { vm.uiState.value.problem == PartiesProblem.OFFLINE }
        assertEquals(1, vm.uiState.value.parties.size)
    }
}
