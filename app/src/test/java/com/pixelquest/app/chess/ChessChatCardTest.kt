package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.chess.domain.Challenge
import com.pixelquest.app.chess.domain.ChallengeStatus
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.ChessChatCard
import com.pixelquest.app.chess.ui.ChessChatTags
import com.pixelquest.app.chess.ui.ChessPartyState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** An open challenge in the study chat, which any other member can take (J152). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChessChatCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val t = Instant.parse("2026-10-22T10:00:00Z")
    private val open = Challenge("c1", "p", "sara", null, TimeControl.FIVE, ColourChoice.RANDOM, true, ChallengeStatus.OPEN, t.plusSeconds(600), t)

    @Test
    fun anotherMember_canAccept() {
        val answers = mutableListOf<Pair<String, Boolean>>()
        composeTestRule.setContent {
            PixelQuestTheme { ChessChatCard("chess_open_challenge", "c1", ChessPartyState(me = "me", challenges = listOf(open), names = mapOf("sara" to "sara")), onAnswer = { id, a -> answers += id to a }) }
        }
        composeTestRule.onNodeWithText("@sara wants a game: 5 min, Rated.").assertExists()
        composeTestRule.onNodeWithTag(ChessChatTags.accept("c1")).performClick()
        assertEquals(listOf("c1" to true), answers)
    }

    @Test
    fun aTakenOne_saysSo() {
        composeTestRule.setContent {
            PixelQuestTheme { ChessChatCard("chess_open_challenge", "c1", ChessPartyState(me = "me"), onAnswer = { _, _ -> }) }
        }
        composeTestRule.onNodeWithText("Taken, or no longer open.").assertExists()
    }

    @Test
    fun aResult_andAGameToWatch() {
        val needed = mutableListOf<String>()
        val opened = mutableListOf<String>()
        val done = com.pixelquest.app.chess.domain.ChessGame("g1", "p", "ali", "sara", TimeControl.THREE, true, com.pixelquest.app.chess.domain.GameStatus.FINISHED,
            com.pixelquest.app.chess.domain.GameResult.BLACK, com.pixelquest.app.chess.domain.ResultReason.CHECKMATE, "fen", com.pixelquest.app.chess.domain.ChessColour.WHITE, 1, 1, 9, t, t, t, t)
        val state = ChessPartyState(me = "me", names = mapOf("ali" to "ali", "sara" to "sara"), games = mapOf("g1" to done))
        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    ChessChatCard("chess_game_result", "g1", state, { _, _ -> }, onNeedGame = { needed += it })
                    ChessChatCard("chess_game_started", "g2", state, { _, _ -> }, onNeedGame = { needed += it }, onOpenGame = { opened += it })
                }
            }
        }
        composeTestRule.onNodeWithText("@sara beat @ali").assertExists()
        composeTestRule.onNodeWithText("By checkmate.").assertExists()
        composeTestRule.onNodeWithTag(ChessChatTags.watch("g2")).performClick()
        assertEquals(listOf("g2"), opened)
        assertEquals(setOf("g1", "g2"), needed.toSet())
    }
}
