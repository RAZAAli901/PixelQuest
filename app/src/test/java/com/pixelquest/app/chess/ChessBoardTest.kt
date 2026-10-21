package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.pixelquest.app.chess.domain.BoardPosition
import com.pixelquest.app.chess.domain.squareIndex
import com.pixelquest.app.chess.domain.squareName
import com.pixelquest.app.chess.ui.BoardMarks
import com.pixelquest.app.chess.ui.ChessBoard
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The board draws any position from its FEN (J139). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessBoardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fen_isRead_squareBySquare() {
        val start = BoardPosition.of("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
        assertEquals('P', start["e4"])
        assertNull(start["e2"])
        assertEquals('k', start["e8"])
        assertEquals(false, start.whiteToMove)
        assertEquals("e1", start.kingOf(white = true))
        assertEquals(BoardPosition.EMPTY, BoardPosition.of("not a fen"))
        assertEquals(BoardPosition.EMPTY, BoardPosition.of("9/8/8/8/8/8/8/8 w - - 0 1"))
        assertEquals(36, squareIndex("e4"))
        assertEquals("e4", squareName(36))
        assertNull(squareIndex("i9"))
    }

    @Test
    fun theBoard_hasItsSixtyFourSquares() {
        composeTestRule.setContent {
            PixelQuestTheme { ChessBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", marks = BoardMarks(lastMove = setOf("e2", "e4"), dots = setOf("e3"))) }
        }
        composeTestRule.onNodeWithTag(ChessBoardTags.BOARD).assertExists()
        composeTestRule.onAllNodesWithTag(ChessBoardTags.square("a1")).assertCountEquals(1)
        composeTestRule.onNodeWithTag(ChessBoardTags.square("h8")).assertExists()
    }
}

/** Flipped, Black sits at the bottom (J140). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessBoardFlipTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun flipped_putsTheEighthRankAtTheBottom() {
        var flipped by androidx.compose.runtime.mutableStateOf(false)
        composeTestRule.setContent { PixelQuestTheme { ChessBoard("8/8/8/8/8/8/8/8 w - - 0 1", flipped = flipped) } }
        fun top(square: String) = composeTestRule.onNodeWithTag(ChessBoardTags.square(square)).fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(top("a1") > top("a8"))
        flipped = true
        composeTestRule.waitForIdle()
        org.junit.Assert.assertTrue(top("a1") < top("a8"))
    }
}
