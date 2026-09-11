package com.pixelquest.app.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.QuestUser
import com.pixelquest.app.domain.DisplayNameModerator
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DisplayNameModerationRejectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displayName_moderation_rejectsOffensiveWordsAndAllowsCleanNames() {
        val testUser = QuestUser(
            id = "hero_user_101",
            email = "hero101@gmail.com",
            displayName = "Hero 101"
        )

        var accountState by mutableStateOf(
            AccountUiState(
                isOptedIn = false,
                displayNameInput = "",
                displayNameError = null
            )
        )

        var optInToggled = false

        composeTestRule.setContent {
            PixelQuestTheme {
                AccountContent(
                    authState = AuthUiState.SignedIn(testUser),
                    accountState = accountState,
                    onSignInWithGoogle = {},
                    onSignOut = {},
                    onDisplayNameChange = { newName ->
                        val error = DisplayNameModerator.validate(newName)
                        accountState = accountState.copy(
                            displayNameInput = newName,
                            displayNameError = error
                        )
                    },
                    onOptInToggle = { optIn ->
                        if (accountState.displayNameError == null && accountState.displayNameInput.isNotBlank()) {
                            optInToggled = true
                            accountState = accountState.copy(isOptedIn = optIn)
                        }
                    },
                    onManualSync = {},
                    onConfirmLeaveLeaderboard = {},
                    onConfirmDeleteCloudAccount = {},
                    onNavigateBack = {}
                )
            }
        }

        // 1. Initial State: Leaderboard participation card is visible
        composeTestRule.onNodeWithText("🏆 LEADERBOARD PARTICIPATION").assertIsDisplayed()
        composeTestRule.onNodeWithText("STATUS: INACTIVE (DEFAULT OFF)").assertIsDisplayed()

        // 2. Input an offensive/disallowed display name with leetspeak evasion
        composeTestRule.onNodeWithText("Enter public pseudonym").performTextInput("Bad$h!t")

        // 3. Verify client-side moderation error is caught and displayed
        assertNotNull(accountState.displayNameError)
        assertEquals("Display name contains disallowed or offensive language.", accountState.displayNameError)
        composeTestRule.onNodeWithText("Display name contains disallowed or offensive language.").assertIsDisplayed()

        // 4. Verify attempting to join leaderboard with offensive name does NOT opt in
        composeTestRule.onNodeWithText("🟢 JOIN LEADERBOARD (OPT IN)").performClick()
        assertEquals(false, optInToggled)
        assertEquals(false, accountState.isOptedIn)

        // 5. Replace with a clean, respectful pseudonym
        composeTestRule.onNodeWithText("Bad$h!t").performTextReplacement("RetroPaladin")

        // 6. Verify error is cleared
        assertNull(accountState.displayNameError)

        // 7. Click Join Leaderboard and verify opt-in succeeds
        composeTestRule.onNodeWithText("🟢 JOIN LEADERBOARD (OPT IN)").performClick()
        assertTrue(optInToggled)
        assertTrue(accountState.isOptedIn)
    }
}
