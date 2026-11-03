package com.pixelquest.app.party.ui

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.ui.info.PartyInfoViewModel
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.ZoneId

/**
 * V049: Party core flow and limits suite.
 *
 * User flow:
 * - Leader tools: rename party, transfer leadership
 * - Leave: member leaves party cleanly
 * - Remove: leader removes a member
 * - Delete: leader deletes the party with name confirmation
 *
 * Out-of-bounds cases:
 * - Leader leaves: blocked in UI and refused by server (leader must hand over first)
 * - Member tries leader actions: UI controls blocked and server returns 403 not_leader
 * - Removed member keeps stale screen open: membership updates to removed/left and screen dismisses gracefully
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PartyCoreFlowAndLimitsTest {

    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r")
    }
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

    private inner class Phone(val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val parties = DaroodPartyRepositoryImpl(asPlayer(userId), db.daroodDao(), { userId }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
        val account = DaroodAccount { userId }
    }

    private fun asPlayer(userId: String): DaroodPartyRemoteDataSource = java.lang.reflect.Proxy.newProxyInstance(
        DaroodPartyRemoteDataSource::class.java.classLoader,
        arrayOf(DaroodPartyRemoteDataSource::class.java),
    ) { _, method, args ->
        synchronized(server) {
            server.me = userId
            method.invoke(server, *(args ?: emptyArray()))
        }
    } as DaroodPartyRemoteDataSource

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            if (condition()) return@runBlocking
            delay(20)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun userFlow_leaderTools_leave_remove_and_delete() = runBlocking {
        val aria = Phone("aria-id")
        val bram = Phone("bram-id")
        val cleo = Phone("cleo-id")

        // 1. Seed party with Aria as leader and Bram and Cleo as members
        val partyId = server.seedParty("Math Circle", "aria-id", "bram-id", "cleo-id", type = PartyType.DAROOD)
        aria.parties.refreshParties()
        aria.parties.refreshParty(partyId)

        val ariaInfo = viewModels.keep(PartyInfoViewModel::class.java) {
            PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to partyId)), aria.parties, account = aria.account)
        }
        val ariaWatching = ariaInfo.uiState.launchIn(collectors)
        waitFor("Aria party loaded") { ariaInfo.uiState.value.party != null && ariaInfo.uiState.value.members.size == 3 }
        assertTrue("Aria is leader", ariaInfo.uiState.value.isLeader)

        // 2. Leader tool: Rename
        ariaInfo.startRename()
        assertNotNull(ariaInfo.uiState.value.newName)
        ariaInfo.onNewNameChange("Advanced Math Circle")
        ariaInfo.confirmRename()
        waitFor("party renamed") {
            server.parties[partyId]?.name == "Advanced Math Circle" && !ariaInfo.uiState.value.busy
        }

        // 3. Leader tool: Transfer leadership to Bram
        val bramMember = ariaInfo.uiState.value.members.first { it.userId == "bram-id" }
        ariaInfo.askToTransfer(bramMember)
        assertEquals(bramMember, ariaInfo.uiState.value.confirmingTransfer)
        ariaInfo.confirmTransfer()
        waitFor("Bram is new leader") {
            server.parties[partyId]?.leaderId == "bram-id" && !ariaInfo.uiState.value.busy
        }
        aria.parties.refreshParty(partyId)
        waitFor("Aria is now regular member") { !ariaInfo.uiState.value.isLeader }

        // 4. Leave: Former leader Aria can now leave the party
        ariaInfo.askToLeave()
        assertTrue(ariaInfo.uiState.value.confirmingLeave)
        ariaInfo.confirmLeave()
        waitFor("Aria left") { ariaInfo.uiState.value.left && !ariaInfo.uiState.value.busy }
        assertEquals("left", server.members.first { it.partyId == partyId && it.userId == "aria-id" }.status)

        // 5. Leader tool: Bram as new leader removes Cleo
        bram.parties.refreshParties()
        bram.parties.refreshParty(partyId)
        val bramInfo = viewModels.keep(PartyInfoViewModel::class.java) {
            PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to partyId)), bram.parties, account = bram.account)
        }
        val bramWatching = bramInfo.uiState.launchIn(collectors)
        waitFor("Bram loaded as leader") { bramInfo.uiState.value.isLeader && bramInfo.uiState.value.members.any { it.userId == "cleo-id" } }

        val cleoMember = bramInfo.uiState.value.members.first { it.userId == "cleo-id" }
        bramInfo.askToRemove(cleoMember)
        assertEquals(cleoMember, bramInfo.uiState.value.confirmingRemoval)
        bramInfo.confirmRemoval()
        waitFor("Cleo removed") {
            server.members.first { it.partyId == partyId && it.userId == "cleo-id" }.status == "removed" && !bramInfo.uiState.value.busy
        }

        // 6. Leader tool: Delete party
        bramInfo.startDelete()
        assertNotNull(bramInfo.uiState.value.deleteConfirmation)
        // Must type matching name to confirm delete
        bramInfo.onDeleteConfirmationChange("Advanced Math Circle")
        assertTrue(bramInfo.uiState.value.deleteConfirmed)
        bramInfo.confirmDelete()
        waitFor("party deleted") { bramInfo.uiState.value.left && server.parties[partyId] == null }
        assertTrue("Party recorded in deleted map", server.deleted.containsKey(partyId))

        ariaWatching.cancel()
        bramWatching.cancel()
    }

    @Test
    fun outOfBounds_leaderLeaves_isBlockedAndRefused() = runBlocking {
        val aria = Phone("aria-id")
        val partyId = server.seedParty("Physics Lab", "aria-id", "bram-id", type = PartyType.DAROOD)
        aria.parties.refreshParties()
        aria.parties.refreshParty(partyId)

        val vm = viewModels.keep(PartyInfoViewModel::class.java) {
            PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to partyId)), aria.parties, account = aria.account)
        }
        val watching = vm.uiState.launchIn(collectors)
        waitFor("party loaded") { vm.uiState.value.party != null && vm.uiState.value.isLeader }

        // 1. UI guard: Leader asking to leave is blocked immediately
        vm.askToLeave()
        assertFalse("Leader cannot be shown leave confirmation dialog", vm.uiState.value.confirmingLeave)

        // 2. Server guard: Direct repository call as leader is refused
        val leaveResult = aria.parties.leaveParty(partyId)
        assertTrue("Leave party as leader must fail", leaveResult is SupabaseResult.ServerError)
        assertEquals(DaroodRefusal.LEADER_MUST_HAND_OVER, DaroodRefusal.of(leaveResult))

        // Leader remains active in party
        assertEquals("active", server.members.first { it.partyId == partyId && it.userId == "aria-id" }.status)
        assertEquals("aria-id", server.parties[partyId]?.leaderId)

        watching.cancel()
    }

    @Test
    fun outOfBounds_memberTriesLeaderActions_isBlockedAndRefused() = runBlocking {
        val aria = Phone("aria-id")
        val bram = Phone("bram-id")
        val partyId = server.seedParty("Biology Group", "aria-id", "bram-id", type = PartyType.DAROOD)
        bram.parties.refreshParties()
        bram.parties.refreshParty(partyId)

        val vm = viewModels.keep(PartyInfoViewModel::class.java) {
            PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to partyId)), bram.parties, account = bram.account)
        }
        val watching = vm.uiState.launchIn(collectors)
        waitFor("party loaded") { vm.uiState.value.party != null && vm.uiState.value.members.size == 2 }
        assertFalse("Bram is not the leader", vm.uiState.value.isLeader)

        val ariaMember = vm.uiState.value.members.first { it.userId == "aria-id" }

        // 1. UI guard: Non-leader cannot remove members
        vm.askToRemove(ariaMember)
        assertNull("Non-leader cannot ask to remove members", vm.uiState.value.confirmingRemoval)

        // 2. UI guard: Non-leader cannot rename party
        vm.startRename()
        assertNull("Non-leader cannot start rename", vm.uiState.value.newName)

        // 3. UI guard: Non-leader cannot transfer leadership
        vm.askToTransfer(ariaMember)
        assertNull("Non-leader cannot transfer leadership", vm.uiState.value.confirmingTransfer)

        // 4. UI guard: Non-leader cannot delete party
        vm.startDelete()
        assertNull("Non-leader cannot start delete", vm.uiState.value.deleteConfirmation)

        // 5. Server guards: Direct calls by non-leader return 403 not_leader
        val renameResult = bram.parties.renameParty(partyId, "Hostile Takeover")
        assertTrue(renameResult is SupabaseResult.ServerError)
        assertEquals(403, (renameResult as SupabaseResult.ServerError).code)
        assertEquals(DaroodRefusal.NOT_LEADER, DaroodRefusal.of(renameResult))

        val removeResult = bram.parties.removeMember(partyId, "aria-id")
        assertTrue(removeResult is SupabaseResult.ServerError)
        assertEquals(403, (removeResult as SupabaseResult.ServerError).code)

        val deleteResult = bram.parties.deleteParty(partyId)
        assertTrue(deleteResult is SupabaseResult.ServerError)
        assertEquals(403, (deleteResult as SupabaseResult.ServerError).code)

        val transferResult = bram.parties.transferLeadership(partyId, "bram-id")
        assertTrue(transferResult is SupabaseResult.ServerError)
        assertEquals(403, (transferResult as SupabaseResult.ServerError).code)

        // Party state intact on server
        assertEquals("Biology Group", server.parties[partyId]?.name)
        assertEquals("aria-id", server.parties[partyId]?.leaderId)

        watching.cancel()
    }

    @Test
    fun outOfBounds_removedMemberKeepsStaleScreenOpen_handledGracefully() = runBlocking {
        val aria = Phone("aria-id")
        val bram = Phone("bram-id")
        val partyId = server.seedParty("Chemistry Circle", "aria-id", "bram-id", type = PartyType.DAROOD)

        bram.parties.refreshParties()
        bram.parties.refreshParty(partyId)

        // Bram opens PartyInfoScreen
        val bramInfo = viewModels.keep(PartyInfoViewModel::class.java) {
            PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to partyId)), bram.parties, account = bram.account)
        }
        val bramWatching = bramInfo.uiState.launchIn(collectors)
        waitFor("Bram opened screen") { bramInfo.uiState.value.party != null && bramInfo.uiState.value.members.size == 2 }

        // Leader Aria removes Bram on server
        val removeResult = asPlayer("aria-id").removeMember(partyId, "bram-id")
        assertTrue(removeResult is SupabaseResult.Success)
        assertEquals("removed", server.members.first { it.partyId == partyId && it.userId == "bram-id" }.status)

        // Bram's phone syncs/refreshes party
        bram.parties.refreshParty(partyId)
        waitFor("Bram no longer in active member list") {
            bramInfo.uiState.value.members.none { it.userId == "bram-id" }
        }

        // Bram attempts member action from stale screen: leaveParty
        bramInfo.askToLeave()
        assertTrue(bramInfo.uiState.value.confirmingLeave)
        bramInfo.confirmLeave()

        // Server refuses since Bram is already removed/not active
        waitFor("problem reported on stale leave") { bramInfo.uiState.value.problem != null }
        assertFalse("Bram not marked as left via user action", bramInfo.uiState.value.left)

        bramWatching.cancel()
    }
}
