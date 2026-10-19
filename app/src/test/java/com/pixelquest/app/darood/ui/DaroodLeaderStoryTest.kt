package com.pixelquest.app.darood.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.ui.create.CreatePartyViewModel
import com.pixelquest.app.darood.ui.info.PartyInfoViewModel
import com.pixelquest.app.darood.ui.invite.AddPeopleViewModel
import com.pixelquest.app.darood.ui.invite.InviteResult
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeUsernameRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

/**
 * The party's view models together, on one server, each player on their own phone (its own cache):
 * creating within the limits, inviting until it's full, removing to make room, accepting, leaving.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodLeaderStoryTest {

    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r")
    }
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = ViewModelStore()
    private var made = 0
    private val collectors = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        // Nothing reads the databases once they close.
        collectors.cancel()
        viewModels.clear()
        Dispatchers.resetMain()
        databases.forEach { it.close() }
    }

    /** A player's phone: its own cache, and the server answering it as its player. */
    private inner class Phone(val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val parties = DaroodPartyRepositoryImpl(asPlayer(userId), db.daroodDao(), { userId }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
        val auth = FakeAuthRepository().apply { userFlow.value = AuthUser(userId, null, null) }
    }

    /**
     * The one server, as [userId]'s phone reaches it. The fake answers without suspending, so each
     * call can be made as that player, whichever phone's refresh runs at the same moment.
     */
    private fun asPlayer(userId: String): DaroodPartyRemoteDataSource = java.lang.reflect.Proxy.newProxyInstance(
        DaroodPartyRemoteDataSource::class.java.classLoader,
        arrayOf(DaroodPartyRemoteDataSource::class.java)
    ) { _, method, args ->
        synchronized(server) {
            server.me = userId
            method.invoke(server, *(args ?: emptyArray()))
        }
    } as DaroodPartyRemoteDataSource

    /** A view model the store clears at the end, so none outlives its database. */
    private inline fun <reified T : ViewModel> keep(noinline make: () -> T): T {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = make() as V
        }
        return ViewModelProvider(viewModels, factory)["vm-${made++}", T::class.java]
    }

    /** The list screen collects its state; so does the test. */
    private fun DaroodPartiesViewModel.collected() = also { vm -> collectors.launch { vm.uiState.collect { } } }

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        repeat(150) {
            if (condition()) return@runBlocking
            delay(20)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun invitesSent() = server.calls.count { it == "invite" }

    @Test
    fun aPartyOfTwo_fillsUp_makesRoom_andPeopleComeAndGo() {
        val aria = Phone("aria-id")
        val bram = Phone("bram-id")
        val cleo = Phone("cleo-id")

        // Create: the limit stays within 2 to 30, whatever is asked.
        val create = keep { CreatePartyViewModel(aria.parties) }
        create.onNameChange("  Friday circle ")
        create.setMaxMembers(1)
        assertEquals(DaroodRules.MEMBERS_MIN, create.uiState.value.maxMembers)
        create.setMaxMembers(31)
        assertEquals(DaroodRules.MEMBERS_MAX, create.uiState.value.maxMembers)
        create.setMaxMembers(2)
        create.create()
        waitFor("the party made") { create.uiState.value.created != null }
        val party = create.uiState.value.created!!
        assertEquals("Friday circle" to 2, server.parties[party]!!.let { it.name to it.maxMembers })

        // Invite: Bram takes the second place. Then it's full, so Cleo's invitation isn't sent.
        val add = keep { AddPeopleViewModel(SavedStateHandle(mapOf("partyId" to party)), FakeUsernameRepository(), aria.parties) }
        waitFor("the limit known") { add.uiState.value.limit == 2 }
        add.invite("bram")
        waitFor("Bram invited, and the party full") { add.uiState.value.result == InviteResult.Sent("bram") && add.uiState.value.isFull }
        add.invite("cleo_r")
        assertEquals("only Bram's sent", 1, invitesSent())

        // Bram accepts on his phone.
        val bramList = keep { DaroodPartiesViewModel(bram.auth, bram.parties) }.collected()
        waitFor("Bram's invitation") { bramList.uiState.value.invites.any { it.id == party } }
        bramList.accept(party)
        waitFor("Bram in") { bramList.uiState.value.parties.any { it.id == party } }

        // Remove: Aria removes Bram, which makes room, and Cleo's invitation goes.
        val info = keep { PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to party)), aria.parties) { "aria-id" } }
        waitFor("both members") { info.uiState.value.members.size == 2 }
        info.askToRemove(info.uiState.value.members.first { it.userId == "bram-id" })
        info.confirmRemoval()
        waitFor("Bram removed") { info.uiState.value.members.map { it.userId } == listOf("aria-id") }
        assertNull(info.uiState.value.problem)
        waitFor("room again") { !add.uiState.value.isFull }
        add.invite("cleo_r")
        waitFor("Cleo invited") { add.uiState.value.result == InviteResult.Sent("cleo_r") }
        assertEquals(2, invitesSent())

        // The party leaves Bram's list.
        bramList.refresh()
        waitFor("gone from Bram's list") { bramList.uiState.value.parties.none { it.id == party } }

        // Leave: Cleo accepts, then leaves; the party leaves her list.
        val cleoList = keep { DaroodPartiesViewModel(cleo.auth, cleo.parties) }.collected()
        waitFor("Cleo's invitation") { cleoList.uiState.value.invites.any { it.id == party } }
        cleoList.accept(party)
        waitFor("Cleo in") { cleoList.uiState.value.parties.any { it.id == party } }
        val cleoInfo = keep { PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to party)), cleo.parties) { "cleo-id" } }
        waitFor("Cleo's info") { cleoInfo.uiState.value.party != null }
        cleoInfo.askToLeave()
        cleoInfo.confirmLeave()
        waitFor("Cleo out") { cleoInfo.uiState.value.left }
        assertEquals("left", server.members.first { it.partyId == party && it.userId == "cleo-id" }.status)
        waitFor("gone from Cleo's list") { cleoList.uiState.value.parties.none { it.id == party } }

        // The leader is never asked to leave, and can't be removed: they hand over or delete.
        info.askToLeave()
        assertFalse(info.uiState.value.confirmingLeave)
        info.askToRemove(info.uiState.value.members.first())
        assertNull(info.uiState.value.confirmingRemoval)
    }
}
