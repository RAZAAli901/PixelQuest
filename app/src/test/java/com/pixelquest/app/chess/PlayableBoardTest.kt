package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.rules.Move
import com.pixelquest.app.chess.rules.Position
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.chess.ui.PlayableBoard
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Moving by tap and by drag (J142). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PlayableBoardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sent = mutableListOf<String>()

    private fun board(colour: ChessColour = ChessColour.WHITE, enabled: Boolean = true, fen: String = Position.START) {
        composeTestRule.setContent {
            PixelQuestTheme { PlayableBoard(fen, colour, enabled, lastMove = null, onMove = { f, t -> sent += Move(f, t).uci }) }
        }
    }

    private fun tap(square: String) = composeTestRule.onNodeWithTag(ChessBoardTags.square(square)).performClick()

    @Test
    fun tap_aPiece_thenItsSquare() {
        board()
        tap("e2")
        tap("e5")
        assertEquals("not a legal move: nothing sent", emptyList<String>(), sent)
        tap("e2")
        tap("e4")
        assertEquals(listOf("e2e4"), sent)
    }

    @Test
    fun theirPieces_andTheirTurn_moveNothing() {
        board(enabled = false)
        tap("e2")
        tap("e4")
        assertEquals(emptyList<String>(), sent)
    }

    @Test
    fun drag_aKnight() {
        board()
        val board = composeTestRule.onNodeWithTag(ChessBoardTags.BOARD)
        board.performTouchInput {
            val sq = width / 8f
            swipe(androidx.compose.ui.geometry.Offset(sq * 6.5f, sq * 7.5f), androidx.compose.ui.geometry.Offset(sq * 5.5f, sq * 5.5f), durationMillis = 400) // g1 to f3
        }
        assertEquals(listOf("g1f3"), sent)
    }

    @Test
    fun asBlack_theBoardIsFlipped() {
        board(colour = ChessColour.BLACK, fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
        tap("e7")
        tap("e5")
        assertEquals(listOf("e7e5"), sent)
    }
}
