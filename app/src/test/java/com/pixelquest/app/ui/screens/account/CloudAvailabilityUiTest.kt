package com.pixelquest.app.ui.screens.account

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.domain.CloudAvailability
import com.pixelquest.app.ui.screens.leaderboard.NotSignedInLeaderboardState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A build without a Supabase project used to offer Sign in with Google, which opened the account
 * picker and then failed against the placeholder server. It now says cloud features aren't set up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class CloudAvailabilityUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theRule() {
        assertTrue(CloudAvailability.isConfigured("https://abcd.supabase.co", "sb_publishable_x"))
        assertFalse(CloudAvailability.isConfigured("https://placeholder-project.supabase.co", "sb_publishable_x"))
        assertFalse(CloudAvailability.isConfigured("https://abcd.supabase.co", "placeholder-anon-key"))
        assertFalse(CloudAvailability.isConfigured("", ""))
    }

    @Test
    fun account_withoutCloud_offersNoSignIn() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(authState = AuthUiState.SignedOut, accountState = AccountUiState(), cloudAvailable = false)
            }
        }
        composeTestRule.onNodeWithText(CloudAvailability.NOT_IN_THIS_BUILD).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("🌐 SIGN IN WITH GOOGLE").fetchSemanticsNodes().size)
    }

    @Test
    fun account_withCloud_offersSignIn() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(authState = AuthUiState.SignedOut, accountState = AccountUiState(), cloudAvailable = true)
            }
        }
        composeTestRule.onNodeWithText("🌐 SIGN IN WITH GOOGLE").assertIsDisplayed()
    }

    @Test
    fun leaderboard_withoutCloud_saysSo_insteadOfSendingYouToSignIn() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                NotSignedInLeaderboardState(onNavigateToAccount = {}, cloudAvailable = false)
            }
        }
        composeTestRule.onNodeWithText(CloudAvailability.NOT_IN_THIS_BUILD).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("🔑 SIGN IN VIA ACCOUNT").fetchSemanticsNodes().size)
    }
}
