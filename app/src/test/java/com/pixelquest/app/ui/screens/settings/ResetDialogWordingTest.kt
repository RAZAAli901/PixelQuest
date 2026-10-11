package com.pixelquest.app.ui.screens.settings

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** RESET ALL PROGRESS says the leaderboard starts over too (Day 33: the reset now reaches the board). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ResetDialogWordingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theFirstWarning_mentionsTheLeaderboard() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                ResetProgressDialogSequence(step = 1, onNextStep = {}, onConfirmWipe = {}, onDismiss = {})
            }
        }
        composeTestRule.onNodeWithText("your numbers there start over too", substring = true).assertExists()
    }
}
