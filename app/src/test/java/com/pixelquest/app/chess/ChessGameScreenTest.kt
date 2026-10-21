package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.rules.Position
import com.pixelquest.app.chess.ui.ChessClockTags
import com.pixelquest.app.chess.ui.ChessGameActions
import com.pixelquest.app.chess.ui.ChessGameScreen
import com.pixelquest.app.chess.ui.ChessGameTags
import com.pixelquest.app.chess.ui.ChessSessionState
import com.pixelquest.app.chess.ui.SessionPhase
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The game screen (J145 on): both clocks beside their players, a low clock warns. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessGameScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val t0 = Instant.parse("2026-10-22T10:00:00Z")
    private val game = ChessGame(
        "g", "p", "me", "sara", TimeControl.THREE, true, GameStatus.ACTIVE, null, null, Position.START, ChessColour.WHITE,
        whiteMs = 132_000, blackMs = 9_000, ply = 4, lastMoveAt = t0, createdAt = t0, startedAt = t0, endedAt = null
    )

    private val did = mutableListOf<String>()

    fun show(state: ChessSessionState) {
        val actions = ChessGameActions(onResign = { did += "resign" }, onOfferDraw = { did += "offer" }, onAnswerDraw = { did += "answer $it" })
        composeTestRule.setContent { PixelQuestTheme { ChessGameScreen(state, ClockSync(), actions) } }
    }

    @Test
    fun bothClocks_andALowOne() {
        show(ChessSessionState(SessionPhase.ACTIVE, game.copy(status = GameStatus.FINISHED, result = com.pixelquest.app.chess.domain.GameResult.WHITE), me = "me", myColour = ChessColour.WHITE, watchers = 4, names = mapOf("me" to "raza", "sara" to "sara"), points = mapOf("sara" to 1102)))
        composeTestRule.onNodeWithTag(ChessClockTags.clock(ChessColour.WHITE)).assertExists()
        composeTestRule.onNodeWithText("2:12").assertExists()
        composeTestRule.onNodeWithText("@raza (you)").assertExists()
        composeTestRule.onNodeWithText("1,102").assertExists()
        composeTestRule.onNodeWithTag(ChessGameTags.KICKER).assertTextContains("3 MIN · RATED")
        composeTestRule.onNodeWithTag(ChessGameTags.WATCHERS).assertExists()
    }

    @Test
    fun underTenSeconds_theClockWarns() {
        show(ChessSessionState(SessionPhase.ACTIVE, game.copy(turn = ChessColour.BLACK, ply = 1), me = "me", myColour = ChessColour.WHITE))
        composeTestRule.onNodeWithTag(ChessClockTags.low(ChessColour.BLACK)).assertExists()
    }

    @Test
    fun watching_saysSo() {
        show(ChessSessionState(SessionPhase.ACTIVE, game, me = "cleo", myColour = null))
        composeTestRule.onNodeWithTag(ChessGameTags.SPECTATING).assertExists()
    }

    @Test
    fun theMoves_inStandardNotation() {
        val moves = listOf("e4", "e5", "Bc4").mapIndexed { i, san -> com.pixelquest.app.chess.domain.ChessMove("g", i + 1, san, "a2a3", "fen", 1, 1, t0) }
        show(ChessSessionState(SessionPhase.ACTIVE, game, moves = moves, me = "me", myColour = ChessColour.WHITE))
        composeTestRule.onNodeWithTag(com.pixelquest.app.chess.ui.MoveListTags.move(3)).assertExists()
        composeTestRule.onNodeWithText("2.").assertExists()
        composeTestRule.onNodeWithText("Bc4").assertExists()
    }

    @Test
    fun resignAsksFirst_andTheirDrawOfferIsABanner() {
        show(ChessSessionState(SessionPhase.ACTIVE, game.copy(drawOfferedBy = "sara"), me = "me", myColour = ChessColour.WHITE))
        composeTestRule.onNodeWithTag(com.pixelquest.app.chess.ui.GameActionTags.DRAW_BANNER).assertExists()
        composeTestRule.onNodeWithTag(com.pixelquest.app.chess.ui.GameActionTags.DECLINE_DRAW).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(com.pixelquest.app.chess.ui.GameActionTags.RESIGN).performScrollTo().performClick()
        org.junit.Assert.assertEquals("asked first", listOf("answer false"), did)
        composeTestRule.onNodeWithText("Resign this game?").assertExists()
        composeTestRule.onAllNodesWithText("RESIGN").onLast().performClick()
        org.junit.Assert.assertEquals(listOf("answer false", "resign"), did)
    }

    @Test
    fun theEnd_resultIntTierRecordAndRematch() {
        val won = game.copy(status = GameStatus.FINISHED, result = com.pixelquest.app.chess.domain.GameResult.WHITE, reason = com.pixelquest.app.chess.domain.ResultReason.CHECKMATE, whiteChange = 14, blackChange = -14, ratingApplied = true)
        val record = listOf(com.pixelquest.app.chess.domain.HeadToHead("me", "sara", null, 5, 1, 2))
        show(ChessSessionState(SessionPhase.FINISHED, won, me = "me", myColour = ChessColour.WHITE, points = mapOf("me" to 905), headToHead = record, names = mapOf("sara" to "sara")))
        composeTestRule.onNodeWithTag(com.pixelquest.app.chess.ui.GameOverTags.CARD).performScrollTo()
        composeTestRule.onNodeWithText("YOU WON").assertExists()
        composeTestRule.onNodeWithText("By checkmate.").assertExists()
        composeTestRule.onNodeWithText("+14 Int").assertExists()
        composeTestRule.onNodeWithText("New tier: Knight").assertExists()
        composeTestRule.onNodeWithText("+1").assertExists()
        composeTestRule.onNodeWithTag(com.pixelquest.app.chess.ui.GameOverTags.REMATCH).assertExists()
    }
}
