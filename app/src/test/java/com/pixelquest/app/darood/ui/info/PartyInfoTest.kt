package com.pixelquest.app.darood.ui.info

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.data.remote.PartyMemberDto
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
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
        // The view models stop reading the database before it closes, and a write already under way
        // finishes: Room's transactions run one at a time, so a task queued after them waits for them.
        viewModels.clear()
        val drained = java.util.concurrent.CountDownLatch(1)
        db.transactionExecutor.execute { drained.countDown() }
        drained.await(2, java.util.concurrent.TimeUnit.SECONDS)
        db.closeAfterWrites()
        Dispatchers.resetMain()
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
        composeTestRule.onNodeWithText("FRIDAY CIRCLE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Darood · 2 members, up to 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("@aria_k (you)").assertIsDisplayed()
        // The leader is marked; everyone else is simply a member (mock 4G).
        composeTestRule.onNodeWithText("LEADER").assertIsDisplayed()
        composeTestRule.onNodeWithText("@bram").assertIsDisplayed()
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
        composeTestRule.onNode(hasText("REMOVE") and hasAnyAncestor(isDialog())).performClick()
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

    @Test
    fun aMemberLeaves_afterSayingSo_andThePartyIsGoneFromTheirList() = runTest(dispatcher) {
        server.seedEntry(partyId, "bram-id", 300)
        server.me = "bram-id"
        val vm = viewModel(me = "bram-id")
        waitUntil { vm.uiState.value.party != null && vm.uiState.value.members.size == 3 }

        vm.askToLeave()
        assertTrue(vm.uiState.value.confirmingLeave)
        vm.dismissDialog()
        assertFalse(vm.uiState.value.confirmingLeave)
        assertFalse("nothing sent until it's confirmed", "leaveParty" in server.calls)

        vm.askToLeave()
        vm.confirmLeave()
        waitUntil { vm.uiState.value.left && vm.uiState.value.party == null }
        assertEquals("left", server.members.first { it.partyId == partyId && it.userId == "bram-id" }.status)
        assertEquals("what they posted still counts", 300L, server.total(partyId))
    }

    @Test
    fun theLeader_isntAskedToLeave_theyHandOverFirst() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.party != null }
        vm.askToLeave()
        assertFalse(vm.uiState.value.confirmingLeave)
        vm.confirmLeave()
        advanceUntilIdle()
        assertFalse("leaveParty" in server.calls)
    }

    @Test
    fun aMemberSeesLeave_andTheDialogAsksFirst() {
        var asked = false
        var confirmed = false
        val shown = androidx.compose.runtime.mutableStateOf(state(myId = "bram-id"))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyInfoContent(state = shown.value, onNavigateBack = {}, onLeave = { asked = true }, onConfirmLeave = { confirmed = true })
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_LEAVE_HINT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_LEAVE).performClick()
        assertTrue(asked)

        shown.value = shown.value.copy(confirmingLeave = true)
        composeTestRule.onNodeWithText("Leave Friday circle?", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("stays in its total", substring = true).assertIsDisplayed()
        composeTestRule.onNode(hasText("Leave party", ignoreCase = true) and hasAnyAncestor(isDialog())).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun theLeader_isToldToHandOverFirst_insteadOfLeave() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyInfoContent(
                    state = state(myId = "aria-id").copy(problem = InfoProblem.Refused(com.pixelquest.app.darood.domain.DaroodRefusal.LEADER_MUST_HAND_OVER)),
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_LEAVE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_LEAVE_HINT).assertIsDisplayed()
        composeTestRule.onNodeWithText("Hand the leadership to another member first, or delete the party.").assertIsDisplayed()
    }

    @Test
    fun theLeaderRenamesTheParty_twoTo40Characters_asTheServerInsists() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.party != null }
        vm.startRename()
        assertEquals("Friday circle", vm.uiState.value.newName)

        vm.onNewNameChange(" x ")
        assertEquals(com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_SHORT, vm.uiState.value.newNameProblem)
        vm.confirmRename()
        vm.onNewNameChange("y".repeat(41))
        assertEquals(com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_LONG, vm.uiState.value.newNameProblem)
        vm.confirmRename()
        assertFalse("not sent while it can't be a name", "renameParty" in server.calls)

        vm.onNewNameChange("  Saturday circle ")
        vm.confirmRename()
        waitUntil { vm.uiState.value.party?.name == "Saturday circle" && !vm.uiState.value.busy }
        assertEquals("Saturday circle", server.parties[partyId]?.name)
        assertEquals(null, vm.uiState.value.newName)
    }

    @Test
    fun aMember_cantRename_orHandOver() = runTest(dispatcher) {
        server.me = "bram-id"
        val vm = viewModel(me = "bram-id")
        waitUntil { vm.uiState.value.party != null && vm.uiState.value.members.size == 3 }
        vm.startRename()
        assertEquals(null, vm.uiState.value.newName)
        vm.askToTransfer(vm.uiState.value.members.first { it.userId == "cleo-id" })
        assertEquals(null, vm.uiState.value.confirmingTransfer)
    }

    @Test
    fun theLeaderHandsOver_afterSayingSo_andStaysAsAMemberWhoCanLeave() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.members.size == 3 && vm.uiState.value.party != null }
        vm.askToTransfer(vm.uiState.value.members.first { it.role == PartyRole.LEADER })
        assertEquals("not to themselves", null, vm.uiState.value.confirmingTransfer)

        val cleo = vm.uiState.value.members.first { it.userId == "cleo-id" }
        vm.askToTransfer(cleo)
        assertEquals(cleo, vm.uiState.value.confirmingTransfer)
        vm.confirmTransfer()
        waitUntil { !vm.uiState.value.isLeader && vm.uiState.value.members.firstOrNull()?.userId == "cleo-id" }
        assertEquals("cleo-id", server.parties[partyId]?.leaderId)
        assertEquals(PartyRole.MEMBER, vm.uiState.value.members.first { it.userId == "aria-id" }.role)

        vm.askToLeave()
        assertTrue("a member now, so they may leave", vm.uiState.value.confirmingLeave)
    }

    @Test
    fun theLeaderSeesRename_andMakeLeader_withTheirDialogs() {
        var renaming = false
        var handingTo: PartyMember? = null
        val shown = androidx.compose.runtime.mutableStateOf(state(myId = "aria-id"))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyInfoContent(state = shown.value, onNavigateBack = {}, onRename = { renaming = true }, onTransfer = { handingTo = it })
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.memberTransfer("aria-id")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.memberTransfer("bram-id")).performClick()
        assertEquals("bram-id", handingTo?.userId)
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_RENAME).performClick()
        assertTrue(renaming)

        shown.value = shown.value.copy(confirmingTransfer = handingTo)
        composeTestRule.onNodeWithText("Make @bram the leader?").assertIsDisplayed()
        composeTestRule.onNodeWithText("stay in the party as a member", substring = true).assertIsDisplayed()
    }

    // The dialogs' fields are checked on their own: Robolectric can't settle a dialog window holding a
    // text field (a bare Dialog with a bare BasicTextField never gets idle), and a test stuck like
    // that can run the test JVM out of memory. The dialogs themselves are plain PixelDialogs.
    @Test
    fun theRenameField_saysWhyANameWontDo() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                RenameField(name = "x", problem = com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_SHORT, onNameChange = {})
            }
        }
        composeTestRule.onNodeWithText("PARTY NAME").assertIsDisplayed()
        composeTestRule.onNodeWithText("At least 2 characters.").assertIsDisplayed()
    }

    @Test
    fun aMember_seesNoLeaderTools() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyInfoContent(state = state(myId = "bram-id"), onNavigateBack = {})
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_RENAME).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.memberTransfer("aria-id")).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.memberTransfer("bram-id")).assertDoesNotExist()
    }

    @Test
    fun theLeaderDeletesTheParty_onlyOnceItsNameIsTyped() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.party != null }
        vm.startDelete()
        assertEquals("", vm.uiState.value.deleteConfirmation)

        for (typed in listOf("", "Friday", "friday circle")) {
            vm.onDeleteConfirmationChange(typed)
            assertFalse(typed, vm.uiState.value.deleteConfirmed)
            vm.confirmDelete()
        }
        assertFalse("not sent until the name is typed", "deleteParty" in server.calls)

        vm.onDeleteConfirmationChange(" Friday circle ")
        assertTrue(vm.uiState.value.deleteConfirmed)
        vm.confirmDelete()
        waitUntil { vm.uiState.value.left && vm.uiState.value.party == null }
        assertTrue(partyId in server.deleted)
    }

    @Test
    fun aMember_cantDelete() = runTest(dispatcher) {
        server.me = "bram-id"
        val vm = viewModel(me = "bram-id")
        waitUntil { vm.uiState.value.party != null }
        vm.startDelete()
        assertEquals(null, vm.uiState.value.deleteConfirmation)
    }

    @Test
    fun theLeaderSeesDelete_andAMemberDoesnt() {
        var asked = false
        val shown = androidx.compose.runtime.mutableStateOf(state(myId = "aria-id"))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyInfoContent(state = shown.value, onNavigateBack = {}, onDelete = { asked = true })
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_DELETE).performClick()
        assertTrue(asked)
        shown.value = state(myId = "bram-id")
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_DELETE).assertDoesNotExist()
    }

    @Test
    fun theDeleteConfirmation_asksForTheName_andSaysWhenItDoesntMatch() {
        val typed = androidx.compose.runtime.mutableStateOf("Friday")
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                DeleteConfirmation(typed = typed.value, partyName = "Friday circle", confirmed = typed.value.trim() == "Friday circle", onTypedChange = {})
            }
        }
        composeTestRule.onNodeWithText("can't be undone", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("That's not the party's name.").assertIsDisplayed()
        typed.value = "Friday circle"
        composeTestRule.onNodeWithText("That's not the party's name.").assertDoesNotExist()
    }

    @Test
    fun theLeaderSetsTheMilestone_aboveTheTotal_asTheServerInsists() = runTest(dispatcher) {
        server.seedEntry(partyId, "bram-id", 300)
        val vm = viewModel()
        waitUntil { vm.uiState.value.party?.total == 300L }
        vm.startMilestone()
        assertEquals("no milestone yet: an empty field", "", vm.uiState.value.newMilestone)

        vm.onMilestoneChange("3,00")
        assertEquals("300", vm.uiState.value.newMilestone)
        assertEquals("not above 300", null, vm.uiState.value.newMilestoneValue)
        vm.confirmMilestone()
        assertFalse("not sent", "setMilestone" in server.calls)

        vm.onMilestoneChange("10,000 Darood")
        assertEquals(10_000L, vm.uiState.value.newMilestoneValue)
        vm.confirmMilestone()
        waitUntil { vm.uiState.value.party?.milestone == 10_000L && !vm.uiState.value.busy }
        assertEquals(10_000L, server.parties[partyId]?.milestone)
    }

    @Test
    fun aMember_cantSetIt_andAFinishedParty_keepsIt() = runTest(dispatcher) {
        server.me = "bram-id"
        val asMember = viewModel(me = "bram-id")
        waitUntil { asMember.uiState.value.party != null }
        asMember.startMilestone()
        assertEquals(null, asMember.uiState.value.newMilestone)

        server.me = "aria-id"
        server.parties[partyId] = server.parties.getValue(partyId).copy(milestone = 100, completedAt = server.now.toString())
        val leader = viewModel()
        waitUntil { leader.uiState.value.party?.isFinished == true }
        leader.startMilestone()
        assertEquals(null, leader.uiState.value.newMilestone)
    }

    @Test
    fun theLeaderSeesTheMilestone_andAWayToSetIt_aMemberJustSeesIt() {
        val shown = androidx.compose.runtime.mutableStateOf(state(myId = "aria-id"))
        var asked = false
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyInfoContent(state = shown.value, onNavigateBack = {}, onMilestone = { asked = true })
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_MILESTONE).assertTextEquals("No milestone yet")
        composeTestRule.onNodeWithText("Set milestone").performClick()
        assertTrue(asked)

        shown.value = state(myId = "bram-id").let { it.copy(party = it.party!!.copy(milestone = 10_000)) }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_MILESTONE).assertTextEquals("Milestone: 10,000")
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_SET_MILESTONE).assertDoesNotExist()
    }

    @Test
    fun theMilestoneField_saysWhenItsNotAboveTheTotal() {
        val typed = androidx.compose.runtime.mutableStateOf("2000")
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                MilestoneField(typed = typed.value, total = 2_450, onTypedChange = {})
            }
        }
        composeTestRule.onNodeWithText("It has to be above the party's total, 2,450.").assertExists()
        typed.value = "2451"
        composeTestRule.onNodeWithText("It has to be above the party's total, 2,450.").assertDoesNotExist()
        assertEquals("10000", milestoneDigits("10,000 Darood"))
        assertEquals("313", milestoneDigits("\u06F3\u06F1\u06F3"))
        assertEquals("1234567890", milestoneDigits("123456789012345"))
    }

    @Test
    fun theLeaderPutsThePartyOnTheRaceBoard_andTakesItOff() = runTest(dispatcher) {
        val vm = viewModel()
        waitUntil { vm.uiState.value.party != null }
        assertFalse("off by default", vm.uiState.value.party!!.isPublicRace)
        vm.setRaceBoard(true)
        waitUntil { vm.uiState.value.party?.isPublicRace == true && !vm.uiState.value.busy }
        assertTrue(server.parties.getValue(partyId).isPublicRace)
        vm.setRaceBoard(false)
        waitUntil { vm.uiState.value.party?.isPublicRace == false && !vm.uiState.value.busy }

        server.me = "bram-id"
        val asMember = viewModel(me = "bram-id")
        waitUntil { asMember.uiState.value.party != null }
        val asked = server.calls.count { it == "setRaceBoard" }
        asMember.setRaceBoard(true)
        advanceUntilIdle()
        assertEquals("a member's switch isn't sent", asked, server.calls.count { it == "setRaceBoard" })
        assertFalse(server.parties.getValue(partyId).isPublicRace)
    }

    @Test
    fun theSwitch_saysPlainlyWhatsShared_andAMemberIsTold() {
        var turned: Boolean? = null
        val shown = androidx.compose.runtime.mutableStateOf(state(myId = "aria-id"))
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyInfoContent(state = shown.value, onNavigateBack = {}, onRaceBoard = { turned = it })
            }
        }
        composeTestRule.onNodeWithText("Join the race board").assertExists()
        composeTestRule.onNodeWithText("Never anyone's name or their numbers.", substring = true).assertExists()
        composeTestRule.onNodeWithText("once the party has a milestone", substring = true).assertExists()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_RACE_SWITCH).performScrollTo().performClick()
        assertEquals(true, turned)

        shown.value = state(myId = "bram-id").let { it.copy(party = it.party!!.copy(isPublicRace = true)) }
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_RACE_SWITCH).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.INFO_RACE_NOTE).assertExists()
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
