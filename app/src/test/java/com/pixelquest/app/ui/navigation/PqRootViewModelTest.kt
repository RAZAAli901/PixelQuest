package com.pixelquest.app.ui.navigation

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.data.remote.PartyMemberDto
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

/**
 * The frame's state (docs/ui-v2.md), which the side menu's used to carry: the Deen tab goes with the
 * Islamic section; a waiting Darood invitation dots it, while Darood is available; an invitation of
 * any kind dots the parties buttons.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PqRootViewModelTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "me")
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { ZoneId.of("UTC") })
    private val settings = FakeSettingsRepository()
    private val viewModels = TestViewModels()

    private val vm by lazy { viewModels.keep(PqRootViewModel::class.java) { PqRootViewModel(settings, parties) } }

    // The view model shares its state on Main, which runBlocking would otherwise hold up.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    private fun invite(type: PartyType) {
        server.members += PartyMemberDto(server.seedParty("Group", "u2", type = type), "me", "member", "invited", null, null)
        runBlocking { parties.refreshParties() }
    }

    @Test
    fun byDefault_deenShows_withNoDots() = runBlocking {
        assertEquals(PqRootState(), vm.state.first())
    }

    @Test
    fun deen_hidesWhenThePlayerHidesTheIslamicSection() = runBlocking {
        assertTrue(vm.state.first { it.islamicHubShown }.islamicHubShown)
        settings.setIslamicHubShown(false)
        assertFalse(vm.state.first { !it.islamicHubShown }.islamicHubShown)
    }

    @Test
    fun theState_followsSimpleMode() = runBlocking {
        settings.setSimpleModeEnabled(true)
        assertTrue(vm.state.first { it.isSimpleMode }.isSimpleMode)
    }

    @Test
    fun aDaroodInvitation_dotsDeen_andTheParties() = runBlocking {
        invite(PartyType.DAROOD)
        val state = vm.state.first { it.daroodInviteWaiting }
        assertTrue(state.partyInviteWaiting)
    }

    @Test
    fun aStudyInvitation_dotsTheParties_notDeen() = runBlocking {
        invite(PartyType.STUDY)
        val state = vm.state.first { it.partyInviteWaiting }
        assertFalse(state.daroodInviteWaiting)
    }

    @Test
    fun deensDot_waitsForDaroodToBeAvailable() = runBlocking {
        invite(PartyType.DAROOD)
        assertTrue(vm.state.first { it.daroodInviteWaiting }.daroodInviteWaiting)

        settings.setDaroodAvailable(false)
        val state = vm.state.first { !it.daroodInviteWaiting }
        assertTrue("the invitation still waits in Parties", state.partyInviteWaiting)
    }
}
