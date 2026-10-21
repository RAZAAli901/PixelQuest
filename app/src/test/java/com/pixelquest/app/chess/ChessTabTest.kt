package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.chess.domain.ChessColour
import com.pixelquest.app.chess.domain.ChessGame
import com.pixelquest.app.chess.domain.GameStatus
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.ChallengeTags
import com.pixelquest.app.chess.ui.ChessMember
import com.pixelquest.app.chess.ui.ChessPartyState
import com.pixelquest.app.chess.ui.ChessTab
import com.pixelquest.app.chess.ui.ChessTabActions
import com.pixelquest.app.chess.ui.ChessTabTags
import com.pixelquest.app.chess.ui.LiveGameTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** The study party's Chess tab (J153): challenge, live games, the ladder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessTabTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val t = Instant.parse("2026-10-22T10:00:00Z")

    @Test
    fun liveGames_theLadder_andAChallengeFromIt() {
        val watched = mutableListOf<String>()
        val picked = mutableListOf<String?>()
        val live = ChessGame("g1", "p", "ali", "sara", TimeControl.FIVE, true, GameStatus.ACTIVE, null, null, "fen", ChessColour.WHITE, 1, 1, 10, t, t, t, null)
        val state = ChessPartyState(
            me = "me",
            members = listOf(ChessMember("sara", "sara"), ChessMember("ali", "ali")),
            liveGames = listOf(live),
            leaderboard = listOf(Rating("sara", TimeControl.FIVE, 1102, 20, 12, 3, 5, 1110), Rating("me", TimeControl.FIVE, 1060, 19, 9, 4, 6, 1070)),
            names = mapOf("sara" to "sara", "ali" to "ali", "me" to "raza")
        )
        composeTestRule.setContent { PixelQuestTheme { ChessTab(state, ChessTabActions(onWatch = { watched += it }, onPick = { picked += it })) } }
        composeTestRule.onNodeWithText("@ali vs @sara").assertExists()
        composeTestRule.onNodeWithTag(LiveGameTags.watch("g1")).performClick()
        assertEquals(listOf("g1"), watched)
        composeTestRule.onNodeWithTag(ChessTabTags.row("sara")).assertExists()
        composeTestRule.onNodeWithText("12W · 3D · 5L").assertExists()
        composeTestRule.onNodeWithText("@raza (you)").assertExists()
        composeTestRule.onNodeWithTag(ChessTabTags.CHALLENGE).performClick()
        composeTestRule.onNodeWithTag(ChallengeTags.SHEET).assertExists()
    }
}
