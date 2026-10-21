package com.pixelquest.app.study.ui

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.data.remote.DaroodPartyRemoteDataSource
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.MemoryStudyTimerStore
import com.pixelquest.app.study.data.StudyRepositoryImpl
import com.pixelquest.app.study.data.remote.StudyRemoteDataSource
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeStudyServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 * A study party through its screens, on two phones sharing one server: Aria, who leads it, adds a
 * pinned quiz; Bram finds it on This week, ticks it off and logs study time; the board puts him
 * first, with his medal, on both phones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyPartyFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("Asia/Karachi")
    private val server = FakeDaroodServer(me = null).apply {
        usernames += mapOf("aria" to "aria_k", "bram" to "bram")
        now = Instant.parse("2026-10-21T05:00:00Z")
    }
    private val study = FakeStudyServer(server)
    private val party = server.seedParty("Physics group", "aria", "bram", type = PartyType.STUDY)
    private val databases = mutableListOf<AppDatabase>()
    private val viewModels = TestViewModels()

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

    /** A player's phone: its own cache, and the server answering it as its player. */
    private inner class Phone(userId: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .allowMainThreadQueries().build().also { databases += it }
        val parties = DaroodPartyRepositoryImpl(asPlayer<DaroodPartyRemoteDataSource>(userId, server), db.daroodDao(), { userId }, now = { server.now.toEpochMilli() }, zone = { zone })
        val repo = StudyRepositoryImpl(
            asPlayer<StudyRemoteDataSource>(userId, study), db.studyDao(), db.partyDao(), { userId },
            clock = { server.now.toEpochMilli() }, timeZone = { zone }
        )
        val vm: StudyPartyViewModel = viewModels.keep(StudyPartyViewModel::class.java) {
            StudyPartyViewModel(SavedStateHandle(mapOf(StudyPartyViewModel.ARG_PARTY to party)), parties, repo, null, MemoryStudyTimerStore())
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
    fun aQuizAdded_isFinished_andWinsThePlayerAMedal() {
        val aria = Phone("aria")
        var screen by mutableStateOf<@Composable () -> Unit>({})
        composeTestRule.setContent { PixelQuestTheme { screen() } }

        // Aria opens the party and adds a pinned quiz for Friday morning.
        screen = { StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = {}, viewModel = aria.vm) }
        waitFor("Aria's party") { aria.vm.uiState.value.loaded && aria.vm.uiState.value.isLeader }
        // Its click as TalkBack makes it: Robolectric's touch doesn't reliably reach the floating button.
        composeTestRule.onNodeWithTag(StudyTags.ADD_ITEM).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        waitFor("the sheet") { aria.vm.uiState.value.form != null }
        // The sheet's own window is out of Robolectric's reach (as dialogs are): its fields, as typed.
        aria.vm.updateForm(
            aria.vm.uiState.value.form!!.withTitle("Waves quiz").withSubject("Physics")
                .copy(kind = StudyItemKind.QUIZ, dueDay = LocalDate.of(2026, 10, 23), dueTime = LocalTime.of(9, 0), pinned = true)
        )
        aria.vm.saveForm()
        waitFor("added") { aria.vm.uiState.value.form == null && study.items.isNotEmpty() }
        val quiz = study.items.values.single()
        waitFor("pinned on her screen") { shown(StudyTags.pinned(quiz.id)) }

        // Bram opens the party on his phone, finds it on This week, and ticks it off two days early.
        val bram = Phone("bram")
        screen = { StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = {}, viewModel = bram.vm) }
        waitFor("Bram's This week") { shown(StudyTags.itemDone(quiz.id)) }
        composeTestRule.onNodeWithText("Waves quiz", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(StudyTags.itemDone(quiz.id), useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        waitFor("done") { shown(StudyTags.itemUndo(quiz.id)) && study.completions.size == 1 }

        // And logs 40 minutes by hand.
        composeTestRule.onNodeWithTag(StudyTags.minutes(45)).performClick()
        composeTestRule.onNodeWithTag(StudyTags.LOG_MINUTES).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        waitFor("logged") { study.sessions.singleOrNull()?.minutes == 45 }

        // The board: 15 for the quiz, 5 for being early, 4 for the study time, and gold.
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.SCOREBOARD)).performClick()
        waitFor("his points") { bram.vm.uiState.value.board.firstOrNull()?.let { it.userId == "bram" && it.points == 24 } == true }
        assertEquals(Medal.GOLD, bram.vm.uiState.value.board.first().let { medalFor(it.place, it.points) })

        // Aria's phone, refreshed, agrees.
        aria.vm.refresh()
        waitFor("Aria sees it") { aria.vm.uiState.value.board.first().let { it.userId == "bram" && it.points == 24 } }
        assertEquals(1, aria.vm.uiState.value.items.single().doneCount)
    }

    @Test
    @Config(fontScale = 2.0f)
    fun atLargeText_everyTabAndChip_staysATapTarget() {
        val bram = Phone("bram")
        study.seedItem(party, "aria", "Lab report")
        composeTestRule.setContent { PixelQuestTheme { StudyPartyScreen(onNavigateBack = {}, onOpenInfo = {}, onAddPeople = {}, viewModel = bram.vm) } }
        waitFor("loaded") { bram.vm.uiState.value.items.isNotEmpty() }
        val tags = PartyTab.entries.filter { it != PartyTab.CHAT }.map { StudyTags.tabButton(it) } + StudyTags.minutes(25)
        tags.forEach { tag ->
            val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
            val minimum = with(node.layoutInfo.density) { 48.dp.roundToPx() }
            assertTrue("$tag is ${node.size.height}px high", node.size.height >= minimum)
        }
        // The last tab is reached by scrolling the row sideways, and still opens.
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.PEOPLE)).performClick()
        waitFor("People") { bram.vm.uiState.value.tab == PartyTab.PEOPLE }
    }
}
