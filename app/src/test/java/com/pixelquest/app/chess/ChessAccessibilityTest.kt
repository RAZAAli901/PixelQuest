package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessMove
import com.pixelquest.app.chess.rules.Position
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.chess.ui.ChessLook
import com.pixelquest.app.chess.ui.PlayableBoard
import com.pixelquest.app.chess.ui.moveSaid
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The board for TalkBack: squares in words, a move by its actions, moves said aloud; contrast (J165). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun squaresInWords_andAMoveByTalkBack() {
        val sent = mutableListOf<String>()
        composeTestRule.setContent { PixelQuestTheme { PlayableBoard(Position.START, ChessColour.WHITE, enabled = true, lastMove = null, onMove = { sent += it }) } }
        composeTestRule.onNodeWithContentDescription("e2, white pawn").assertExists()
        composeTestRule.onNodeWithContentDescription("e4, empty").assertExists()
        composeTestRule.onNodeWithTag(ChessBoardTags.square("g1")).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.onNodeWithContentDescription("f3, empty, a move here").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        composeTestRule.onNodeWithTag(ChessBoardTags.square("f3")).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("g1f3"), sent)
    }

    @Test
    fun movesInWords() {
        val said = mutableListOf<String>()
        composeTestRule.setContent {
            PixelQuestTheme {
                said += moveSaid(ChessMove("g", 1, "Nf3", "g1f3", "", 0, 0, Instant.EPOCH))
                said += moveSaid(ChessMove("g", 4, "exd5+", "e6d5", "", 0, 0, Instant.EPOCH))
                said += moveSaid(ChessMove("g", 7, "Qxf7#", "h5f7", "", 0, 0, Instant.EPOCH))
                said += moveSaid(ChessMove("g", 9, "O-O", "e1g1", "", 0, 0, Instant.EPOCH))
                said += moveSaid(ChessMove("g", 11, "a8=Q", "a7a8q", "", 0, 0, Instant.EPOCH))
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(
            listOf("White knight to f3", "Black pawn takes on d5, check", "White queen takes on f7, checkmate", "White castles king side", "White pawn to a8, becomes a queen"),
            said.take(5)
        )
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun inkPieces_standOutOnEverySquare_inBothSkins() {
        var mode by androidx.compose.runtime.mutableStateOf(ThemeMode.Comic)
        val colours = mutableMapOf<ThemeMode, List<Color>>()
        composeTestRule.setContent { PixelQuestTheme(themeMode = mode) { colours[mode] = listOf(PqColors.Ink, ChessLook.light, ChessLook.dark, ChessLook.highlight) } }
        composeTestRule.waitForIdle()
        mode = ThemeMode.Pixel
        composeTestRule.waitForIdle()
        for ((skin, list) in colours) {
            val (ink, light, dark, highlight) = list
            for (square in listOf(light, dark, highlight)) assertTrue("$skin: ${contrast(ink, square)}", contrast(ink, square) >= 4.5)
        }
        assertEquals(2, colours.size)
    }
}
