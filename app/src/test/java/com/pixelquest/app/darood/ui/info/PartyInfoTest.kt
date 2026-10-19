package com.pixelquest.app.darood.ui.info

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.PartyMemberDto
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.chat.PartyChatScreen
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import java.time.Instant

/** A party's info: its active members, the leader first, each with the day they joined. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PartyInfoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        // The view models stop reading the database before it closes.
        viewModels.clear()
        Dispatchers.resetMain()
        db.close()
    }

    private val viewModels = androidx.lifecycle.ViewModelStore()
    private var made = 0

    private val db = androidx.room.Room.inMemoryDatabaseBuilder(
        androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>(),
        com.pixelquest.app.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    private val server = FakeDaroodServer(me = "aria-id").apply {
        usernames += mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r", "dara-id" to "dara")
    }
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })
    private val partyId = server.seedParty("Friday circle", "aria-id", maxMembers = 5).also { id ->
        server.members += PartyMemberDto(id, "cleo-id", "member", "active", "2026-10-01T09:00:00Z", "2026-10-03T09:00:00Z")
        server.members += PartyMemberDto(id, "bram-id", "member", "active", "2026-10-01T09:00:00Z", "2026-10-02T09:00:00Z")
        server.members += PartyMemberDto(id, "dara-id", "member", "invited", "2026-10-01T09:00:00Z", null)
        server.members += PartyMemberDto(id, "eli-id", "member", "left", "2026-10-01T09:00:00Z", "2026-10-02T09:00:00Z")
    }

    private fun viewModel(me: String? = "aria-id"): PartyInfoViewModel {
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PartyInfoViewModel(SavedStateHandle(mapOf("partyId" to partyId)), parties) { me } as T
        }
        // Each its own key: a second view model isn't the first one again.
        return androidx.lifecycle.ViewModelProvider(viewModels, factory)["info-${made++}", PartyInfoViewModel::class.java]
    }

    @Test
    fun theMembers_areTheActiveOnes_leaderFirst_thenByWhenTheyJoined() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.members.size == 3 && vm.uiState.value.party != null }
        val state = vm.uiState.value
        assertEquals(listOf("aria-id", "bram-id", "cleo-id"), state.members.map { it.userId })
        assertEquals(PartyRole.LEADER, state.members.first().role)
        assertEquals("Friday circle", state.party?.name)
        assertTrue(state.isLeader)
    }

    @Test
    fun aMember_isntTheLeader() = runTest(dispatcher) {
        server.me = "bram-id"
        val vm = viewModel(me = "bram-id")
        waitUntil { vm.uiState.value.party != null }
        assertFalse(vm.uiState.value.isLeader)
    }

    @Test
    fun theScreen_listsEachMember_withRoleAndJoinedDate_andTheLeaderCanAddPeople() {
        var added = false
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyInfoContent(state = state(myId = "aria-id"), onNavigateBack = {}, onAddPeople = { added = true })
            }
        }
        composeTestRule.onNodeWithText("Friday circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 members, up to 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("@aria_k (you)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leader").assertIsDisplayed()
        composeTestRule.onNodeWithText("@bram").assertIsDisplayed()
        composeTestRule.onNodeWithText("Member").assertIsDisplayed()
        composeTestRule.onNodeWithText("Joined Oct 2, 2026").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_ADD_PEOPLE).performClick()
        assertTrue(added)
    }

    @Test
    fun aMember_seesNoAddPeople() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyInfoContent(state = state(myId = "bram-id"), onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithText("@bram (you)").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_ADD_PEOPLE).assertDoesNotExist()
    }

    @Test
    fun theChatsTitle_opensTheInfo() {
        var opened = false
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = { opened = true })
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_TITLE).performClick()
        assertTrue(opened)
    }

    @Test
    fun theLeaderRemovesAMember_afterSayingSo_andWhatTheyPostedStaysInTheTotal() = runTest(dispatcher) {
        server.seedEntry(partyId, "bram-id", 300)
        val vm = viewModel()
        waitUntil { vm.uiState.value.members.size == 3 && vm.uiState.value.party?.total == 300L }
        val bram = vm.uiState.value.members.first { it.userId == "bram-id" }

        vm.askToRemove(bram)
        assertEquals(bram, vm.uiState.value.confirmingRemoval)
        vm.dismissDialog()
        assertEquals(null, vm.uiState.value.confirmingRemoval)
        assertFalse("nothing sent until it's confirmed", "removeMember" in server.calls)

        vm.askToRemove(bram)
        vm.confirmRemoval()
        waitUntil { vm.uiState.value.members.none { it.userId == "bram-id" } && !vm.uiState.value.busy }
        assertEquals("removed", server.members.first { it.partyId == partyId && it.userId == "bram-id" }.status)
        assertEquals(listOf("aria-id", "cleo-id"), vm.uiState.value.members.map { it.userId })
        assertEquals(300L, vm.uiState.value.party?.total)
        assertEquals(null, vm.uiState.value.problem)
    }

    @Test
    fun nobodyRemovesTheLeader_andAMemberRemovesNobody() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.members.size == 3 && vm.uiState.value.party != null }
        vm.askToRemove(vm.uiState.value.members.first { it.role == PartyRole.LEADER })
        assertEquals(null, vm.uiState.value.confirmingRemoval)

        server.me = "bram-id"
        val asMember = viewModel(me = "bram-id")
        waitUntil { asMember.uiState.value.party != null && asMember.uiState.value.members.size == 3 }
        asMember.askToRemove(asMember.uiState.value.members.first { it.userId == "cleo-id" })
        assertEquals(null, asMember.uiState.value.confirmingRemoval)
    }

    @Test
    fun offline_theMemberStays_andTheScreenSaysWhy() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.members.size == 3 && vm.uiState.value.party != null }
        server.offline = true
        vm.askToRemove(vm.uiState.value.members.first { it.userId == "bram-id" })
        vm.confirmRemoval()
        waitUntil { vm.uiState.value.problem != null }
        assertEquals(InfoProblem.Offline, vm.uiState.value.problem)
        assertEquals(3, vm.uiState.value.members.size)
    }

    @Test
    fun theLeaderSeesRemove_onMembersOnly_andTheDialogAsksFirst() {
        var asked: PartyMember? = null
        var confirmed = false
        val shown = androidx.compose.runtime.mutableStateOf(state(myId = "aria-id"))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyInfoContent(
                    state = shown.value,
                    onNavigateBack = {},
                    onRemove = { asked = it },
                    onConfirmRemoval = { confirmed = true }
                )
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.memberRemove("aria-id")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.memberRemove("bram-id")).performClick()
        assertEquals("bram-id", asked?.userId)

        shown.value = shown.value.copy(confirmingRemoval = asked)
        composeTestRule.onNodeWithText("Remove @bram?").assertIsDisplayed()
        composeTestRule.onNodeWithText("stays in the party's total", substring = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun aMember_seesNoRemove_andAProblemIsSaid() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyInfoContent(state = state(myId = "bram-id").copy(problem = InfoProblem.Offline), onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.memberRemove("aria-id")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_PROBLEM).assertIsDisplayed()
        composeTestRule.onNodeWithText("nothing changed", substring = true).assertIsDisplayed()
    }

    private fun state(myId: String) = PartyInfoUiState(
        party = Party(partyId, "Friday circle", "aria-id", 5, null, null, null, Instant.parse("2026-10-01T12:00:00Z")),
        members = listOf(
            PartyMember(partyId, "aria-id", "aria_k", PartyRole.LEADER, MemberStatus.ACTIVE, null, Instant.parse("2026-10-01T12:00:00Z")),
            PartyMember(partyId, "bram-id", "bram", PartyRole.MEMBER, MemberStatus.ACTIVE, null, Instant.parse("2026-10-02T12:00:00Z"))
        ),
        myId = myId
    )

    /** Room answers on its own threads: real time, a little at a time. */
    private suspend fun TestScope.waitUntil(condition: () -> Boolean) {
        repeat(200) {
            advanceUntilIdle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("still waiting")
    }
}
