package com.pixelquest.app.darood.ui.invite

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.darood.data.MyUsername
import com.pixelquest.app.darood.domain.Username
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.testing.FakeUsernameCache
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.testing.FakeUsernameRepository
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Adding people: an exact username, looked up once the typing pauses. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AddPeopleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private val usernames = FakeUsernameRepository(taken = mutableSetOf("bram"))
    private val auth = FakeAuthRepository().apply { userFlow.value = AuthUser("aria-id", null, null) }
    private val cache = FakeUsernameCache().apply { save("aria-id", Username.parse("aria_k")!!) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }


    private val db = androidx.room.Room.inMemoryDatabaseBuilder(
        androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>(),
        com.pixelquest.app.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    private val server = com.pixelquest.app.testing.FakeDaroodServer(me = "aria-id").apply {
        usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r")
    }
    private val parties = com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl(
        server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") }
    )
    private val partyId = server.seedParty("Friday circle", "aria-id", maxMembers = 3)

    @After
    fun tearDown() {
        // In order: the view models stop, their flows finish ending on the test's dispatcher, and the
        // database closes once Room is done with it.
        viewModels.clear()
        dispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        db.closeAfterWrites()
    }

    private val viewModels = com.pixelquest.app.testing.TestViewModels()

    private fun viewModel() = viewModels.keep(AddPeopleViewModel::class.java) {
        AddPeopleViewModel(SavedStateHandle(mapOf("partyId" to partyId)), usernames, parties, MyUsername(auth, cache))
    }

    @Test
    fun aTakenName_isSomeoneToInvite_andTheServerIsAskedOnceTheTypingPauses() = runTest(dispatcher) {
        val vm = viewModel()
        "bram".indices.forEach { i ->
            vm.onQueryChange("Bram".take(i + 1))
            advanceTimeBy(100)
        }
        runCurrent()
        assertTrue(usernames.availabilityChecks.isEmpty())

        advanceTimeBy(AddPeopleViewModel.LOOKUP_DELAY_MS)
        runCurrent()
        assertEquals(listOf("bram"), usernames.availabilityChecks)
        assertEquals(Lookup.Found(Username.parse("bram")!!), vm.uiState.value.lookup)
    }

    @Test
    fun aFreeName_isNobody_ownNameIsYou_andABrokenOneSaysWhy() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onQueryChange("cleo")
        advanceUntilIdle()
        assertEquals(Lookup.NotFound(Username.parse("cleo")!!), vm.uiState.value.lookup)

        vm.onQueryChange("@Aria_K")
        advanceUntilIdle()
        assertEquals(Lookup.Yours, vm.uiState.value.lookup)

        vm.onQueryChange("ab")
        advanceUntilIdle()
        assertEquals(Lookup.Invalid(UsernameProblem.TOO_SHORT), vm.uiState.value.lookup)
        assertEquals("never asked about yourself or a broken name", listOf("cleo"), usernames.availabilityChecks)

        usernames.offline = true
        vm.onQueryChange("dara")
        advanceUntilIdle()
        assertEquals(Lookup.Unknown, vm.uiState.value.lookup)
    }

    @Test
    fun theScreen_showsARowToInvite_andWhyNotOtherwise() {
        var invited: String? = null
        val state = mutableListOf(
            AddPeopleUiState(query = "bram", lookup = Lookup.Found(Username.parse("bram")!!))
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                AddPeopleContent(state = state.first(), onQueryChange = {}, onNavigateBack = {}, onInvite = { invited = it })
            }
        }
        composeTestRule.onNodeWithText("@bram").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_INVITE).performClick()
        assertEquals("bram", invited)
    }

    @Test
    fun theScreen_saysWhenNobodyHasTheName() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AddPeopleContent(state = AddPeopleUiState("cleo", Lookup.NotFound(Username.parse("cleo")!!)), onQueryChange = {}, onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithText("Nobody uses @cleo. Check the spelling with your friend.").assertIsDisplayed()
    }

    @Test
    fun inviting_sendsIt_andThePeopleShowWhereTheyStand() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        waitUntil { vm.uiState.value.people.size == 1 }

        vm.invite("bram")
        waitUntil { vm.uiState.value.result != null && vm.uiState.value.people.size == 2 }
        assertEquals(InviteResult.Sent("bram"), vm.uiState.value.result)
        assertEquals(
            listOf("aria_k" to com.pixelquest.app.party.domain.MemberStatus.ACTIVE, "bram" to com.pixelquest.app.party.domain.MemberStatus.INVITED),
            vm.uiState.value.people.map { it.username to it.status }
        )

        // Bram declines on his phone; the next invitation brings the change.
        server.members.replaceAll { if (it.userId == "bram-id") it.copy(status = "declined") else it }
        vm.invite("cleo_r")
        waitUntil {
            vm.uiState.value.result != null &&
                vm.uiState.value.people.any { it.username == "bram" && it.status == com.pixelquest.app.party.domain.MemberStatus.DECLINED }
        }
        assertEquals(InviteResult.Sent("cleo_r"), vm.uiState.value.result)
    }

    @Test
    fun aFullParty_orNoConnection_isSaid() = runTest(dispatcher) {
        server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(partyId, "x1", "member", "active", null, null)
        server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(partyId, "x2", "member", "invited", null, null)
        val vm = viewModel()
        vm.invite("bram")
        waitUntil { vm.uiState.value.result != null }
        assertEquals(InviteResult.Refused(com.pixelquest.app.darood.domain.DaroodRefusal.PARTY_FULL), vm.uiState.value.result)

        server.offline = true
        vm.invite("cleo_r")
        waitUntil { vm.uiState.value.result == InviteResult.Offline }
    }

    @Test
    fun theScreen_showsBadges_andTheResult() {
        val people = listOf(
            com.pixelquest.app.party.domain.PartyMember(partyId, "aria-id", "aria_k", com.pixelquest.app.party.domain.PartyRole.LEADER, com.pixelquest.app.party.domain.MemberStatus.ACTIVE, null, null),
            com.pixelquest.app.party.domain.PartyMember(partyId, "bram-id", "bram", com.pixelquest.app.party.domain.PartyRole.MEMBER, com.pixelquest.app.party.domain.MemberStatus.INVITED, null, null),
            com.pixelquest.app.party.domain.PartyMember(partyId, "cleo-id", "cleo_r", com.pixelquest.app.party.domain.PartyRole.MEMBER, com.pixelquest.app.party.domain.MemberStatus.DECLINED, null, null),
            com.pixelquest.app.party.domain.PartyMember(partyId, "dan-id", "dan", com.pixelquest.app.party.domain.PartyRole.MEMBER, com.pixelquest.app.party.domain.MemberStatus.ACTIVE, null, null)
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Light) {
                AddPeopleContent(state = AddPeopleUiState(people = people, result = InviteResult.Sent("bram")), onQueryChange = {}, onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithText("Invitation sent to @bram.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leader").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invited").assertIsDisplayed()
        composeTestRule.onNodeWithText("Declined").assertIsDisplayed()
        composeTestRule.onNodeWithText("Joined").assertIsDisplayed()
    }

    @Test
    fun aFullParty_stopsInviting_beforeTheServerIsAsked() = runTest(dispatcher) {
        // 3 places: the leader, a member and an invitation.
        server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(partyId, "x1", "member", "active", null, null)
        server.members += com.pixelquest.app.party.data.remote.PartyMemberDto(partyId, "x2", "member", "invited", null, null)
        val vm = viewModel()
        waitUntil { vm.uiState.value.isFull }
        assertEquals(3, vm.uiState.value.placesTaken)

        val callsBefore = server.calls.count { it == "invite" }
        vm.invite("bram")
        advanceUntilIdle()
        assertEquals("not sent", callsBefore, server.calls.count { it == "invite" })

        // Someone declines: there's room again.
        server.members.replaceAll { if (it.userId == "x2") it.copy(status = "declined") else it }
        parties.refreshParty(partyId)
        waitUntil { !vm.uiState.value.isFull }
    }

    @Test
    fun theScreen_saysItsFull_andInviteWaits() {
        val people = (1..10).map {
            com.pixelquest.app.party.domain.PartyMember(partyId, "u$it", "user$it", com.pixelquest.app.party.domain.PartyRole.MEMBER, com.pixelquest.app.party.domain.MemberStatus.ACTIVE, null, null)
        }
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                AddPeopleContent(
                    state = AddPeopleUiState(query = "bram", lookup = Lookup.Found(Username.parse("bram")!!), people = people, limit = 10),
                    onQueryChange = {},
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_FULL).assertIsDisplayed()
        composeTestRule.onNodeWithText("10 of 10 places are taken", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_INVITE).assertIsNotEnabled()
    }

    @Test
    fun the21stInvitationInAnHour_isRefused_andSaysWhy() = runTest(dispatcher) {
        repeat(20) { server.inviteLog += "aria-id" to server.now }
        val vm = viewModel()
        vm.invite("bram")
        waitUntil { vm.uiState.value.result != null }
        assertEquals(InviteResult.Refused(com.pixelquest.app.darood.domain.DaroodRefusal.TOO_MANY_INVITES), vm.uiState.value.result)
    }

    /** Room answers on its own threads: real time, a little at a time. */
    private suspend fun kotlinx.coroutines.test.TestScope.waitUntil(condition: () -> Boolean) {
        repeat(200) {
            advanceUntilIdle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("still waiting")
    }
}
