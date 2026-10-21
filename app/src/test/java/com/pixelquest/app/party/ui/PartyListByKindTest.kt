package com.pixelquest.app.party.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.ui.DaroodIslamicItem
import com.pixelquest.app.darood.ui.DaroodPartyKind
import com.pixelquest.app.darood.ui.list.DaroodPartiesUiState
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.party.data.remote.PartyMemberDto
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.ui.StudyPartyKind
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
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Each kind's list shows its own: the Islamic hub's Darood Parties, and the study parties for the
 * main app's Parties. Their invitations too, and the hub's badge counts Darood's alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PartyListByKindTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "me").apply {
        usernames["me"] = "aria_k"
        usernames["sam"] = "sam_w"
    }
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })
    private val auth = FakeAuthRepository().apply { initialUser = AuthUser(id = "me", email = "aria@example.com", displayName = "Aria") }
    private val viewModels = TestViewModels()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server.seedParty("Friday circle", "me")
        server.seedParty("Physics group", "me", type = PartyType.STUDY)
        val darood = server.seedParty("Night circle", "sam")
        val study = server.seedParty("Exam group", "sam", type = PartyType.STUDY)
        for (party in listOf(darood, study)) server.members += PartyMemberDto(party, "me", "member", "invited", "2026-10-19T10:00:00Z", null)
        repository.refreshParties()
        Unit
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    private fun list(type: String?): DaroodPartiesViewModel = viewModels.keep(DaroodPartiesViewModel::class.java) {
        DaroodPartiesViewModel(auth, repository, null, SavedStateHandle(listOfNotNull(type?.let { DaroodPartiesViewModel.ARG_TYPE to it }).toMap()))
    }

    private fun settled(vm: DaroodPartiesViewModel): DaroodPartiesUiState = runBlocking {
        withTimeout(5_000) { vm.uiState.first { it.signedIn == true && it.parties.isNotEmpty() } }
    }

    @Test
    fun withoutAKind_theListIsEveryKind_asOnTheHub() {
        val vm = list(null)
        val state = settled(vm)

        assertEquals(null, vm.type)
        assertEquals(setOf("Friday circle", "Physics group"), state.parties.map { it.name }.toSet())
        assertEquals(setOf("Night circle", "Exam group"), state.invites.map { it.name }.toSet())
    }

    @Test
    fun theStudyList_isStudyParties_andTheirInvitations() {
        val vm = list("study")
        val state = settled(vm)

        assertEquals(PartyType.STUDY, vm.type)
        assertEquals(listOf("Physics group"), state.parties.map { it.name })
        assertEquals(listOf("Exam group"), state.invites.map { it.name })
    }

    @Test
    fun theIslamicHubsBadge_countsDaroodInvitationsAlone() = runBlocking {
        val item = DaroodIslamicItem(FakeSettingsRepository(), repository)
        assertEquals(1, withTimeout(5_000) { item.item().first() }.badgeCount)
    }
}
