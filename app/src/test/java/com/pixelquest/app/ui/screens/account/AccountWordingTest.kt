package com.pixelquest.app.ui.screens.account

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Account's wording fits players who signed in by email, not only Google. Seen on the emulator: a
 * signed-in player was still invited to sign in, and the leaderboard consent spoke of "your Google
 * email".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AccountWordingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signedIn_theHeaderNoLongerInvitesYouToSignIn() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                AccountContent(
                    authState = AuthUiState.SignedIn(AuthUser("user-1", "hero@pixelquest.test", null)),
                    accountState = AccountUiState(),
                    cloudAvailable = true
                )
            }
        }
        composeTestRule.onNodeWithText("You're signed in", substring = true).assertExists()
        assertEquals(0, composeTestRule.onAllNodesWithText("Sign in with Google or your email", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("Google email", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun theLeaderboardConsent_doesntAssumeGoogle() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardPrivacyConfirmDialog(displayName = "Titan_Slayer", onConfirm = {}, onDismiss = {})
            }
        }
        composeTestRule.onNodeWithText("Your email, real name", substring = true).assertExists()
        assertEquals(0, composeTestRule.onAllNodesWithText("Google email", substring = true).fetchSemanticsNodes().size)
    }
}
