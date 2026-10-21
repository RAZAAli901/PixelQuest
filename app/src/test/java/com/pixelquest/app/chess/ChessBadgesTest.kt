package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.ChessProfileCard
import com.pixelquest.app.chess.ui.TierBadge
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Int points and the tier, on the profile and beside names (J158). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChessBadgesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theBadge_andTheProfileCard() {
        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    TierBadge(1180, "sara")
                    ChessProfileCard(listOf(Rating("me", TimeControl.FIVE, 1180, 14, 8, 2, 4, 1210), Rating("me", TimeControl.ONE, 960, 6, 3, 0, 3, 990)))
                }
            }
        }
        composeTestRule.onNodeWithContentDescription("Chess: 1,180 Int, Bishop").assertExists()
        composeTestRule.onNodeWithText("Chess Int · best tier Bishop").assertExists()
        composeTestRule.onNodeWithText("11W · 2D · 7L").assertExists()
        composeTestRule.onAllNodesWithText("800").assertCountEquals(2) // 3 and 10 minutes, not yet played
    }
}
