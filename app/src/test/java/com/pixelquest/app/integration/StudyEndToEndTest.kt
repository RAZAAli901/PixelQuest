package com.pixelquest.app.integration

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.create.CreatePartyScreen
import com.pixelquest.app.darood.ui.create.CreatePartyViewModel
import com.pixelquest.app.darood.ui.invite.AddPeopleScreen
import com.pixelquest.app.darood.ui.invite.AddPeopleViewModel
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.ui.PartiesScreen
import com.pixelquest.app.study.data.MemoryStudyTimerStore
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.data.remote.StudyRemoteDataSource
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.ui.StudyPartyScreen
import com.pixelquest.app.study.ui.StudyPartyViewModel
import com.pixelquest.app.study.ui.StudyTags
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.FakeUsernameRepository
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A study party from the start, through the screens, on two phones sharing one server: Aria starts
 * one from Parties and invites Bram by his username; he accepts from his Parties list; she adds a
 * quiz and pins it; he ticks it off; and the scoreboard puts him first, on both phones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T05:00:00Z")
    }
    private val study = FakeStudyServer(server)
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = TestViewModels()
    /** The study kind, as the parties/ routes give it to the list and the Create form (both read "type"). */
    private fun studyKind() = SavedStateHandle(mapOf(DaroodPartiesViewModel.ARG_TYPE to PartyType.STUDY.wire))

    @After
    fun tearDown() {
        viewModels.clear()
        databases.forEach { it.closeAfterWrites() }
    }

    private inline fun <reified T> asPlayer(userId: String, target: T): T = java.lang.reflect.Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java)
    ) { _, method, args ->
        synchronized(server) {
            server.me = userId
            method.invoke(target, *(args ?: emptyArray()))
        }
    } as T

    /** A player's phone: signed in, its own cache, and the server answering it as its player. */
    private inner class Phone(val userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val auth = FakeAuthRepository().apply { userFlow.value = AuthUser(userId, null, null) }
        val parties = DaroodPartyRepositoryImpl(asPlayer<DaroodPartyRemoteDataSource>(userId, server), db.daroodDao(), { userId }, now = { server.now.toEpochMilli() }, zone = { zone })
        val study = StudyRepositoryImpl(
            asPlayer<StudyRemoteDataSource>(userId, this@StudyEndToEndTest.study), db.studyDao(), db.partyDao(), { userId },
            clock = { server.now.toEpochMilli() }, timeZone = { zone }
        )

        fun partyScreen(partyId: String) = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to partyId)), parties, study, null, MemoryStudyTimerStore())
        }
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(250) {
            composeTestRule.waitForIdle()
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(10)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun shown(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun createInvitePinCompleteAndSeeTheBoard() {
        val aria = Phone("aria")
        val bram = Phone("bram")
        var screen by mutableStateOf<@Composable () -> Unit>({})
        composeTestRule.setContent { PixelQuestTheme { screen() } }

        // Aria starts a study party from Parties.
        var creating = false
        val ariaList = viewModels.keep(DaroodPartiesViewModel::class.java) { DaroodPartiesViewModel(aria.auth, aria.parties, null, studyKind()) }
        screen = { PartiesScreen(onSignIn = {}, onOpenParty = {}, onCreateParty = { creating = true }, viewModel = ariaList) }
        waitFor("Aria's Parties") { shown(com.pixelquest.app.darood.ui.DaroodTestTags.CREATE) }
        composeTestRule.onNodeWithTag(com.pixelquest.app.darood.ui.DaroodTestTags.CREATE).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("the form asked for") { creating }

        var party: String? = null
        val create = viewModels.keep(CreatePartyViewModel::class.java) { CreatePartyViewModel(aria.parties, studyKind()) }
        screen = { CreatePartyScreen(onNavigateBack = {}, onCreated = { id, _ -> party = id }, viewModel = create) }
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.CREATE_NAME))).performTextInput("Physics group")
        composeTestRule.onNodeWithTag(DaroodTestTags.CREATE_BUTTON).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("the party") { party != null }
        val id = party!!
        assertEquals("study", server.parties.getValue(id).partyType)

        // From its People tab, she adds Bram by his username.
        val ariaParty = aria.partyScreen(id)
        var addingPeople = false
        screen = { StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = { addingPeople = true }, viewModel = ariaParty) }
        waitFor("her party, as its leader") { ariaParty.uiState.value.isLeader }
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.PEOPLE)).performClick()
        waitFor("Add people") { shown(StudyTags.ADD_PEOPLE) }
        composeTestRule.onNodeWithTag(StudyTags.ADD_PEOPLE).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("adding people") { addingPeople }
        val add = viewModels.keep(AddPeopleViewModel::class.java) {
            AddPeopleViewModel(SavedStateHandle(mapOf("partyId" to id)), FakeUsernameRepository(taken = mutableSetOf("bram")), aria.parties)
        }
        screen = { AddPeopleScreen(onNavigateBack = {}, viewModel = add) }
        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(DaroodTestTags.ADD_SEARCH))).performTextInput("bram")
        waitFor("Bram found") { shown(DaroodTestTags.ADD_INVITE) }
        composeTestRule.onNodeWithTag(DaroodTestTags.ADD_INVITE).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("Bram invited") { server.members.any { it.partyId == id && it.userId == "bram" && it.status == "invited" } }

        // Bram accepts, from his Parties list.
        val bramList = viewModels.keep(DaroodPartiesViewModel::class.java) { DaroodPartiesViewModel(bram.auth, bram.parties, null, studyKind()) }
        screen = { PartiesScreen(onSignIn = {}, onOpenParty = {}, onCreateParty = {}, viewModel = bramList) }
        waitFor("his invitation") { shown(DaroodTestTags.inviteAccept(id)) }
        composeTestRule.onNodeWithTag(DaroodTestTags.inviteAccept(id)).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("Bram in") { server.members.any { it.partyId == id && it.userId == "bram" && it.status == "active" } }

        // Aria adds a quiz for Friday and pins it (the sheet's window is out of Robolectric's reach).
        screen = { StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = {}, viewModel = ariaParty) }
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.THIS_WEEK)).performClick()
        composeTestRule.onNodeWithTag(StudyTags.ADD_ITEM).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("the sheet") { ariaParty.uiState.value.form != null }
        ariaParty.updateForm(
            ariaParty.uiState.value.form!!.withTitle("Waves quiz")
                .copy(kind = StudyItemKind.QUIZ, dueDay = LocalDate.of(2026, 10, 23), dueTime = LocalTime.of(9, 0), pinned = true)
        )
        ariaParty.saveForm()
        waitFor("added") { study.items.isNotEmpty() }
        val quiz = study.items.values.single()
        waitFor("pinned") { shown(StudyTags.pinned(quiz.id)) }

        // Bram opens the party and ticks the quiz off.
        val bramParty = bram.partyScreen(id)
        screen = { StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = {}, viewModel = bramParty) }
        waitFor("the quiz on his week") { shown(StudyTags.itemDone(quiz.id)) }
        composeTestRule.onNodeWithTag(StudyTags.itemDone(quiz.id), useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
        waitFor("ticked") { study.completions.size == 1 && shown(StudyTags.itemUndo(quiz.id)) }

        // The scoreboard: Bram first with 20 (15 for the quiz, 5 for two days early), gold; Aria second.
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.SCOREBOARD)).performClick()
        waitFor("his line") { shown(StudyTags.boardRow("bram")) }
        composeTestRule.onNodeWithText("🥇", useUnmergedTree = true).assertExists()
        assertEquals(listOf("bram" to 20, "aria" to 0), bramParty.uiState.value.board.map { it.userId to it.points })

        // Aria's phone agrees once it refreshes.
        ariaParty.refresh()
        waitFor("Aria's board") { ariaParty.uiState.value.board.firstOrNull()?.let { it.userId == "bram" && it.points == 20 } == true }
    }
}
