package com.pixelquest.app.chess

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.pixelquest.app.chess.data.fake.FakeChessBackend
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.chess.ui.ChessClockTags
import com.pixelquest.app.chess.ui.ChessGameActions
import com.pixelquest.app.chess.ui.ChessGameScreen
import com.pixelquest.app.chess.ui.ChessSessionViewModel
import com.pixelquest.app.chess.ui.GameActionTags
import com.pixelquest.app.chess.ui.GameOverTags
import com.pixelquest.app.testing.TestViewModels
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

/**
 * A whole game on screen against the scripted backend (J166): a legal move goes, an illegal one
 * doesn't, both clocks show, the opponent answers, and checkmate (or resigning) ends it with the
 * end card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessGameFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModels = TestViewModels()

    @After
    fun tearDown() = viewModels.clear()

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(400) {
            composeTestRule.waitForIdle()
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
        throw AssertionError("still waiting: $what")
    }

    private fun play(game: String = "fake-game-mate"): Pair<FakeChessBackend, ChessSessionViewModel> {
        val fake = FakeChessBackend(replyDelayMs = 10)
        val vm = viewModels.keep(ChessSessionViewModel::class.java) {
            ChessSessionViewModel(SavedStateHandle(mapOf(ChessSessionViewModel.ARG_GAME to game)), fake, fake, { FakeChessBackend.ME }, ratings = fake)
        }
        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PixelQuestTheme {
                ChessGameScreen(state, vm.clockSync, ChessGameActions(onMove = vm::move, onResign = vm::resign, onOfferDraw = vm::offerDraw))
            }
        }
        waitFor("loaded") { vm.state.value.game != null }
        return fake to vm
    }

    private fun tap(square: String) = composeTestRule.onNodeWithTag(ChessBoardTags.square(square)).performClick()

    @Test
    fun legalAndIllegalMoves_theClocks_andCheckmateEndsIt() {
        val (_, vm) = play()
        composeTestRule.onNodeWithTag(ChessClockTags.clock(ChessColour.WHITE)).assertExists()
        composeTestRule.onNodeWithTag(ChessClockTags.clock(ChessColour.BLACK)).assertExists()
        // Illegal: a pawn three squares. Nothing is sent.
        tap("e2"); tap("e5")
        assertEquals(0, vm.state.value.game!!.ply)
        // Legal, and the opponent answers from the script.
        for ((n, move) in listOf("e2" to "e4", "f1" to "c4", "d1" to "h5", "h5" to "f7").withIndex()) {
            waitFor("my turn ${n + 1}") { vm.state.value.myTurn && vm.state.value.game!!.ply == n * 2 }
            tap(move.first); tap(move.second)
        }
        waitFor("checkmate") { vm.state.value.game!!.status == GameStatus.FINISHED }
        // The board's topple and stamp play first; the card follows about 1.2 s later.
        waitFor("the end card") { composeTestRule.mainClock.advanceTimeBy(200); composeTestRule.onAllNodesWithTag(GameOverTags.CARD).fetchSemanticsNodes().isNotEmpty() }
        composeTestRule.onNodeWithTag(GameOverTags.CARD).performScrollTo()
        composeTestRule.onNodeWithText("YOU WON").assertExists()
        composeTestRule.onNodeWithText("By checkmate.").assertExists()
        composeTestRule.onNodeWithText("+20 Int").assertExists()
        assertEquals("Qxf7#", vm.state.value.moves.last().san)
    }

    @Test
    fun resigning_asksFirst_thenEndsIt() {
        val (_, vm) = play()
        composeTestRule.onNodeWithTag(GameActionTags.RESIGN).performScrollTo().performClick()
        composeTestRule.onAllNodesWithText("RESIGN").onLast().performClick()
        waitFor("resigned") { vm.state.value.game!!.status == GameStatus.FINISHED }
        composeTestRule.onNodeWithTag(GameOverTags.CARD).performScrollTo()
        composeTestRule.onNodeWithText("YOU LOST").assertExists()
        composeTestRule.onNodeWithText("By resignation.").assertExists()
        assertTrue(vm.state.value.game!!.reason != null)
    }
}
