package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.ClockSync
import com.pixelquest.app.chess.domain.GameResult
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.ResultReason
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.rules.Position
import com.pixelquest.app.chess.ui.ChessBoardTags
import com.pixelquest.app.chess.ui.ChessGameActions
import com.pixelquest.app.chess.ui.ChessGameScreen
import com.pixelquest.app.chess.ui.ChessLook
import com.pixelquest.app.chess.ui.ChessPartyState
import com.pixelquest.app.chess.ui.ChessSessionState
import com.pixelquest.app.chess.ui.ChessTab
import com.pixelquest.app.chess.ui.ChessTabActions
import com.pixelquest.app.chess.ui.ChessTabTags
import com.pixelquest.app.chess.ui.SessionPhase
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The chess screens in each look, and in Simple Mode (J164). The board keeps its design in each. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessLookTest(private val mode: ThemeMode) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun modes() = listOf(ThemeMode.Pixel, ThemeMode.Comic).map { arrayOf<Any>(it) }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val t = Instant.parse("2026-10-22T10:00:00Z")
    private val game = ChessGame("g", "p", "me", "sara", TimeControl.THREE, true, GameStatus.FINISHED, GameResult.WHITE, ResultReason.CHECKMATE,
        Position.START, ChessColour.WHITE, 120_000, 90_000, 12, t, t, t, t, whiteChange = 14, blackChange = -14, ratingApplied = true)

    @Test
    fun theGame_drawsInEachLook_withTheDesignsBoard() {
        var dark = androidx.compose.ui.graphics.Color.Unspecified
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                dark = ChessLook.dark
                ChessGameScreen(ChessSessionState(SessionPhase.FINISHED, game, me = "me", myColour = ChessColour.WHITE, points = mapOf("me" to 900)), ClockSync(), ChessGameActions())
            }
        }
        composeTestRule.onNodeWithTag(ChessBoardTags.BOARD).assertExists()
        val designed = if (mode == ThemeMode.Pixel) androidx.compose.ui.graphics.Color(0xFFE8A33D) else androidx.compose.ui.graphics.Color(0xFF8ECAE6)
        assertEquals(designed, dark)
        composeTestRule.onNodeWithText("+14 Int").assertExists()
    }

    @Test
    fun simpleMode_hasNoPoints() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                androidx.compose.foundation.layout.Column {
                    ChessGameScreen(ChessSessionState(SessionPhase.FINISHED, game, me = "me", myColour = ChessColour.WHITE, points = mapOf("me" to 900, "sara" to 1102), simple = true), ClockSync(), ChessGameActions())
                }
            }
        }
        composeTestRule.onNodeWithText("+14 Int").assertDoesNotExist()
        composeTestRule.onNodeWithText("1,102").assertDoesNotExist()
        composeTestRule.onNodeWithText("YOU WON").assertExists()
    }

    @Test
    fun simpleMode_theTabHasNoLadder() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                ChessTab(ChessPartyState(me = "me", simple = true, leaderboard = listOf(Rating("sara", TimeControl.FIVE, 1102, 1, 1, 0, 0, 1102))), ChessTabActions())
            }
        }
        composeTestRule.onNodeWithTag(ChessTabTags.CHALLENGE).assertExists()
        composeTestRule.onNodeWithTag(ChessTabTags.BOARD).assertDoesNotExist()
    }
}
