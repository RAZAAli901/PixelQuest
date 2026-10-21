package com.pixelquest.app.darood.ui.username

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.darood.domain.UsernameProblem
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The username screen in each theme: a free name can be saved, a broken one says why. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class UsernameSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(state: UsernameSetupUiState, mode: ThemeMode) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                UsernameSetupContent(state = state, onInputChange = {}, onSave = {}, onNavigateBack = {})
            }
        }
    }

    @Test
    fun aFreeName_canBeSaved_inComic() {
        show(UsernameSetupUiState(input = "aria_k", availability = UsernameAvailability.AVAILABLE, loadingCurrent = false), ThemeMode.Comic)

        composeTestRule.onNodeWithText("@aria_k is free").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.USERNAME_SAVE).assertIsEnabled()
    }

    @Test
    fun aTakenName_saysSo_inPixel() {
        show(UsernameSetupUiState(input = "bram", availability = UsernameAvailability.TAKEN, loadingCurrent = false), ThemeMode.Pixel)

        composeTestRule.onNodeWithText("@bram is taken. Try another one.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.USERNAME_SAVE).assertIsNotEnabled()
    }

    @Test
    fun aBrokenRule_isExplained_inLight() {
        show(UsernameSetupUiState(input = "admin", problem = UsernameProblem.RESERVED, loadingCurrent = false), ThemeMode.Comic)

        composeTestRule.onNodeWithText("That name is reserved. Try another one.").assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.USERNAME_SAVE).assertIsNotEnabled()
    }
}
