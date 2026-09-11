package com.pixelquest.app.ui.screens.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.PixelAuthUser
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LeaderboardOptOutUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeUser = PixelAuthUser(
        id = "user-1234-abcd",
        email = "hero@pixelquest.test",
        displayName = "PixelHero"
    )

    @Test
    fun optOutButton_isDiscoverableWhenUserIsOptedIn() {
        var optInToggledValue: Boolean? = null

        val optedInState = AccountUiState(
            isOptedIn = true,
            displayNameInput = "SuperKnight"
        )

        composeTestRule.setContent {
            AccountContent(
                authState = AuthUiState.SignedIn(fakeUser),
                accountState = optedInState,
                onOptInToggle = { optInToggledValue = it }
            )
        }

        // Verify status and discoverable Leave Leaderboard action
        composeTestRule.onNodeWithText("STATUS: ACTIVE (OPTED IN)").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔴 LEAVE LEADERBOARD (OPT OUT)").assertIsDisplayed()

        // Clicking triggers opt-out request
        composeTestRule.onNodeWithText("🔴 LEAVE LEADERBOARD (OPT OUT)").performClick()
        assertTrue(optInToggledValue == false)
    }

    @Test
    fun optOutConfirmationDialog_displaysLightweightWarningAndHandlesDismiss() {
        var dismissed = false
        var confirmed = false

        val dialogState = AccountUiState(
            isOptedIn = true,
            displayNameInput = "SuperKnight",
            showOptOutConfirmDialog = true
        )

        composeTestRule.setContent {
            AccountContent(
                authState = AuthUiState.SignedIn(fakeUser),
                accountState = dialogState,
                onDismissOptOutDialog = { dismissed = true },
                onConfirmOptOut = { confirmed = true }
            )
        }

        // Verify single lightweight confirmation dialog
        composeTestRule.onNodeWithText("LEAVE LEADERBOARD?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to leave the leaderboard? Your rank and public display name will no longer be visible to other players. You can rejoin at any time.").assertIsDisplayed()
        composeTestRule.onNodeWithText("STAY").assertIsDisplayed()
        composeTestRule.onNodeWithText("LEAVE").assertIsDisplayed()

        // Test STAY (dismiss)
        composeTestRule.onNodeWithText("STAY").performClick()
        assertTrue(dismissed)
        assertTrue(!confirmed)
    }

    @Test
    fun optOutConfirmationDialog_confirmCallsOptOut() {
        var confirmed = false

        val dialogState = AccountUiState(
            isOptedIn = true,
            displayNameInput = "SuperKnight",
            showOptOutConfirmDialog = true
        )

        composeTestRule.setContent {
            AccountContent(
                authState = AuthUiState.SignedIn(fakeUser),
                accountState = dialogState,
                onConfirmOptOut = { confirmed = true }
            )
        }

        composeTestRule.onNodeWithText("LEAVE").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun optOutSuccessNotice_displaysNoticeAndDismisses() {
        var noticeDismissed = false

        val noticeState = AccountUiState(
            isOptedIn = false,
            displayNameInput = "SuperKnight",
            showOptOutSuccessNotice = true
        )

        composeTestRule.setContent {
            AccountContent(
                authState = AuthUiState.SignedIn(fakeUser),
                accountState = noticeState,
                onDismissOptOutNotice = { noticeDismissed = true }
            )
        }

        // Verify post-opt-out confirmation notice
        composeTestRule.onNodeWithText("LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've left the leaderboard. Your rank and display name have been removed from public rankings.").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()

        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue(noticeDismissed)
    }
}
