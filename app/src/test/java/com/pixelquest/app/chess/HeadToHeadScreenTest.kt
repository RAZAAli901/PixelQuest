package com.pixelquest.app.chess

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.chess.domain.HeadToHead
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.chess.ui.HeadToHeadScreen
import com.pixelquest.app.chess.ui.HeadToHeadState
import com.pixelquest.app.chess.ui.HeadToHeadTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The rivalry (J157): the total, the split bar, by time control. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class HeadToHeadScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun youSevenAliFive_withTwoDraws() {
        val records = listOf(
            HeadToHead("me", "ali", null, 7, 5, 2),
            HeadToHead("me", "ali", TimeControl.THREE, 3, 1, 1)
        )
        composeTestRule.setContent { PixelQuestTheme { HeadToHeadScreen(HeadToHeadState("Ali", records, loaded = true)) {} } }
        composeTestRule.onNodeWithText("You 7 – 5 Ali with 2 draws").assertExists()
        composeTestRule.onNodeWithTag(HeadToHeadTags.BAR).assertExists()
        composeTestRule.onNodeWithText("3–1–1").assertExists()
        composeTestRule.onNodeWithTag(HeadToHeadTags.timeControl(TimeControl.TEN)).assertExists()
    }

    @Test
    fun noGamesYet_saysSo() {
        composeTestRule.setContent { PixelQuestTheme { HeadToHeadScreen(HeadToHeadState("Ali", emptyList(), loaded = true)) {} } }
        composeTestRule.onNodeWithText("No games against Ali yet.").assertExists()
    }
}
