package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.chess.domain.ColourChoice
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.ChallengeSheet
import com.pixelquest.app.chess.ui.ChallengeTags
import com.pixelquest.app.chess.ui.ChessMember
import com.pixelquest.app.chess.ui.ChessPartyState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The challenge sheet (J150): who, the record, time, colour, rated. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChallengeSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pickAMember_seeTheRecord_andSendTheChoices() {
        val sent = mutableListOf<List<Any?>>()
        val picked = mutableListOf<String?>()
        val state = ChessPartyState(me = "me", members = listOf(ChessMember("sara", "sara"), ChessMember("ali", "ali")), preview = listOf(HeadToHead("me", "sara", null, 4, 1, 2)))
        composeTestRule.setContent {
            PixelQuestTheme { ChallengeSheet(state, onPick = { picked += it }, onSend = { to, tc, c, r -> sent += listOf(to, tc, c, r) }, onDismiss = {}) }
        }
        composeTestRule.onNodeWithTag(ChallengeTags.member("sara")).performClick()
        assertEquals(listOf<String?>("sara"), picked)
        composeTestRule.onNodeWithTag(ChallengeTags.RECORD).assertExists()
        composeTestRule.onNodeWithText("You 4 · draws 2 · them 1").assertExists()
        composeTestRule.onNodeWithTag(ChallengeTags.time(TimeControl.TEN)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ChallengeTags.colour(ColourChoice.BLACK)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ChallengeTags.RATED).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(ChallengeTags.SEND).performScrollTo().performClick()
        assertEquals(listOf(listOf("sara", TimeControl.TEN, ColourChoice.BLACK, false)), sent)
    }

    @Test
    fun theWholeRoom_isAChoiceToo() {
        val sent = mutableListOf<String?>()
        composeTestRule.setContent {
            PixelQuestTheme { ChallengeSheet(ChessPartyState(members = listOf(ChessMember("sara", "sara"))), onPick = {}, onSend = { to, _, _, _ -> sent += to }, onDismiss = {}) }
        }
        composeTestRule.onNodeWithTag(ChallengeTags.ROOM).performClick()
        composeTestRule.onNodeWithTag(ChallengeTags.SEND).performScrollTo().performClick()
        assertEquals(listOf<String?>(null), sent)
    }
}
