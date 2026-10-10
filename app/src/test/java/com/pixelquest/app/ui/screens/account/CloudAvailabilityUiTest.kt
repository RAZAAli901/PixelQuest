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
        val clientId = "123-abc.apps.googleusercontent.com"
        assertTrue(CloudAvailability.isConfigured("https://abcd.supabase.co", "sb_publishable_x"))
        assertFalse(CloudAvailability.isConfigured("https://placeholder-project.supabase.co", "sb_publishable_x"))
        assertFalse(CloudAvailability.isConfigured("https://abcd.supabase.co", "placeholder-anon-key"))
        assertFalse(CloudAvailability.isConfigured("", ""))
        // Email-code sign-in needs only the project; Sign in with Google also needs the web client id.
        assertTrue(CloudAvailability.isGoogleConfigured("https://abcd.supabase.co", "sb_publishable_x", clientId))
        assertFalse(CloudAvailability.isGoogleConfigured("https://abcd.supabase.co", "sb_publishable_x", ""))
        assertFalse(CloudAvailability.isGoogleConfigured("https://placeholder-project.supabase.co", "sb_publishable_x", clientId))
        // Found on the emulator: a placeholder showed a Google button that could only fail.
        assertFalse(CloudAvailability.isGoogleConfigured("https://abcd.supabase.co", "sb_publishable_x", "placeholder-google-client-id"))
        assertFalse(CloudAvailability.isGoogleConfigured("https://abcd.supabase.co", "sb_publishable_x", "123-abc"))
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
        assertEquals(0, composeTestRule.onAllNodesWithText("✉️ SIGN IN WITH EMAIL").fetchSemanticsNodes().size)
        // Nothing above it still invites the player to connect or join.
        assertEquals(0, composeTestRule.onAllNodesWithText("Connect your Google account", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("🏆 JOIN THE LEADERBOARD").fetchSemanticsNodes().size)
    }

    @Test
    fun account_withCloud_offersSignIn() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(authState = AuthUiState.SignedOut, accountState = AccountUiState(), cloudAvailable = true, googleAvailable = true)
            }
        }
        composeTestRule.onNodeWithText("🌐 SIGN IN WITH GOOGLE").assertIsDisplayed()
        composeTestRule.onNodeWithText("✉️ SIGN IN WITH EMAIL").assertIsDisplayed()
    }

    @Test
    fun account_withoutTheGoogleClientId_stillOffersEmail() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(authState = AuthUiState.SignedOut, accountState = AccountUiState(), cloudAvailable = true, googleAvailable = false)
            }
        }
        assertEquals(0, composeTestRule.onAllNodesWithText("🌐 SIGN IN WITH GOOGLE").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("✉️ SIGN IN WITH EMAIL").assertIsDisplayed()
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
