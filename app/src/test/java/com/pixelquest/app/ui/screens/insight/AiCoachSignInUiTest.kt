package com.pixelquest.app.ui.screens.insight

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.domain.ai.AiErrorCopy
import com.pixelquest.app.ui.screens.today.TodayAiInsightOptInCard
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Signed out, the AI Coach says it's for signed-in players and its button leads to Account, in every
 * theme, on the AI Coach screen and on Today.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AiCoachSignInUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theAiCoachScreen_offersSignIn_inPixel() = screenOffersSignIn(ThemeMode.Pixel, "SIGN IN")

    @Test
    fun theAiCoachScreen_offersSignIn_inLight() = screenOffersSignIn(ThemeMode.Light, "Sign in")

    @Test
    fun theAiCoachScreen_offersSignIn_inComic() = screenOffersSignIn(ThemeMode.Comic, "SIGN IN")

    private fun screenOffersSignIn(mode: ThemeMode, button: String) {
        var openedAccount = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                AiInsightScreenContent(
                    uiState = AiInsightUiState.SignInRequired(),
                    onRefresh = {},
                    onNavigateToAccount = { openedAccount++ }
                )
            }
        }
        composeTestRule.onNodeWithText(AiErrorCopy.SIGN_IN_REQUIRED).assertExists()
        composeTestRule.onNodeWithText(button).performScrollTo().performClick()
        assertEquals(1, openedAccount)
    }

    @Test
    fun todaysCard_asksToSignIn_insteadOfEnableInSettings() {
        var openedAccount = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                TodayAiInsightOptInCard(themeMode = ThemeMode.Pixel, onEnableClick = { openedAccount++ }, needsSignIn = true)
            }
        }
        composeTestRule.onNodeWithText("ENABLE IN SETTINGS").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sign in with Google or an emailed code", substring = true).assertExists()
        composeTestRule.onNodeWithText("SIGN IN").performClick()
        assertEquals(1, openedAccount)
    }
}
