package com.pixelquest.app.chess

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.chess.data.fake.FakeChessBackend
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.ui.ChallengeTags
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.chess.ui.ChessGameActions
import com.pixelquest.app.chess.ui.ChessGameScreen
import com.pixelquest.app.chess.ui.ChessGameTags
import com.pixelquest.app.chess.ui.ChessPartyViewModel
import com.pixelquest.app.chess.ui.ChessSessionViewModel
import com.pixelquest.app.chess.ui.ChessTab
import com.pixelquest.app.chess.ui.ChessTabActions
import com.pixelquest.app.chess.ui.ChessTabTags
import com.pixelquest.app.chess.ui.IncomingChallengeScreen
import com.pixelquest.app.chess.ui.IncomingChallengeViewModel
import com.pixelquest.app.chess.ui.IncomingTags
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
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
import java.time.ZoneId

/** Challenging, answering a challenge, and watching a game, on screen (J167). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessChallengeFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModels = TestViewModels()
    private val fake = FakeChessBackend(replyDelayMs = 10)
    private val server = FakeDaroodServer(me = FakeChessBackend.ME).apply {
        usernames += mapOf(FakeChessBackend.ME to "raza", FakeChessBackend.OPPONENT to "bram", FakeChessBackend.WATCHED_WHITE to "cleo")
    }
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val parties = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { server.now.toEpochMilli() }, zone = { ZoneId.of("UTC") })

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(400) {
            composeTestRule.waitForIdle()
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
        throw AssertionError("still waiting: $what")
    }

    @Test
    fun aChallenge_fromTheTab_toAMember() {
        val party = server.seedParty("Physics", FakeChessBackend.ME, FakeChessBackend.OPPONENT, FakeChessBackend.WATCHED_WHITE, type = PartyType.STUDY)
        runBlocking { parties.refreshParties(); parties.refreshParty(party) }
        val vm = viewModels.keep(ChessPartyViewModel::class.java) {
            ChessPartyViewModel(SavedStateHandle(mapOf(ChessPartyViewModel.ARG_PARTY to party)), fake, fake, fake, parties, { FakeChessBackend.ME }, chess = fake)
        }
        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PixelQuestTheme { ChessTab(state, ChessTabActions(onChallenge = vm::challenge, onPick = vm::preview, onAnswer = vm::answer)) }
        }
        waitFor("members") { vm.state.value.members.size == 2 }
        composeTestRule.onNodeWithTag(ChessTabTags.CHALLENGE).performClick()
        composeTestRule.onNodeWithTag(ChallengeTags.member(FakeChessBackend.OPPONENT)).performClick()
        waitFor("their record") { vm.state.value.preview.isNotEmpty() }
        composeTestRule.onNodeWithTag(ChallengeTags.SEND).performScrollTo().performClick()
        waitFor("sent") { vm.state.value.challenges.any { it.fromUser == FakeChessBackend.ME && it.toUser == FakeChessBackend.OPPONENT } }
        composeTestRule.onNodeWithText("Your challenge to @bram").assertExists()
    }

    @Test
    fun anIncomingChallenge_isAccepted_andTheGameStarts() {
        val vm = viewModels.keep(IncomingChallengeViewModel::class.java) {
            IncomingChallengeViewModel(SavedStateHandle(mapOf(IncomingChallengeViewModel.ARG to "fake-challenge-1")), fake, fake, parties)
        }
        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PixelQuestTheme { IncomingChallengeScreen(state, onAnswer = vm::answer, onNavigateBack = {}) }
        }
        waitFor("loaded") { vm.state.value.challenge != null }
        composeTestRule.onNodeWithTag(IncomingTags.COUNTDOWN).assertExists()
        composeTestRule.onNodeWithTag(IncomingTags.ACCEPT).performClick()
        waitFor("a game") { vm.state.value.gameId != null }
        val started = runBlocking { (fake.fetchGame(vm.state.value.gameId!!) as com.pixelquest.app.data.remote.SupabaseResult.Success).data.first }
        assertEquals(GameStatus.WAITING, started.status)
    }

    @Test
    fun watching_aGame_readOnly_withTheCount() {
        val vm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to "fake-game-watch")), fake, fake, { FakeChessBackend.ME })
        }
        val moved = mutableListOf<String>()
        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PixelQuestTheme { ChessGameScreen(state, vm.clockSync, ChessGameActions(onMove = { moved += it })) }
        }
        waitFor("watching") { vm.state.value.spectating && vm.state.value.watchers == 2 }
        composeTestRule.onNodeWithTag(ChessGameTags.SPECTATING).assertExists()
        composeTestRule.onNodeWithTag(ChessBoardTags.square("g1")).performClick()
        composeTestRule.onNodeWithTag(ChessBoardTags.square("f3")).performClick()
        assertTrue("a watcher sends nothing", moved.isEmpty())
        composeTestRule.onNodeWithTag(ChessGameTags.LEAVE).performScrollTo().assertExists()
    }
}
