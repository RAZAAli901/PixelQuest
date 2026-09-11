package com.pixelquest.app.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.PixelAuthUser
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardContent
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardItemUiModel
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInOptInLeaderboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun signIn_optIn_setDisplayName_appearsOnLeaderboard() {
        // Step 1: Start at AccountScreen signed out
        var authState by mutableStateOf<AuthUiState>(AuthUiState.SignedOut)
        var accountState by mutableStateOf(AccountUiState(isOptedIn = false))
        var currentScreen by mutableStateOf("ACCOUNT")

        val testUser = PixelAuthUser(
            id = "hero-uid-999",
            email = "paladin@pixelquest.test",
            displayName = "Sir Paladin"
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                if (currentScreen == "ACCOUNT") {
                    AccountContent(
                        authState = authState,
                        accountState = accountState,
                        onSignInWithGoogle = {
                            authState = AuthUiState.SignedIn(testUser)
                        },
                        onDisplayNameChange = {
                            accountState = accountState.copy(displayNameInput = it)
                        },
                        onOptInToggle = {
                            accountState = accountState.copy(showConfirmDialog = true)
                        },
                        onConfirmOptIn = {
                            accountState = accountState.copy(
                                isOptedIn = true,
                                showConfirmDialog = false
                            )
                        },
                        onDismissOptInDialog = {
                            accountState = accountState.copy(showConfirmDialog = false)
                        },
                        onNavigateBack = {
                            currentScreen = "LEADERBOARD"
                        }
                    )
                } else {
                    val leaderboardItem = LeaderboardItemUiModel(
                        rank = 1,
                        id = testUser.id,
                        displayName = accountState.displayNameInput,
                        score = 14,
                        secondaryScore = 14,
                        scoreLabel = "14d",
                        secondaryLabel = "Lvl 5",
                        isCurrentUser = true,
                        isOptedIn = true
                    )
                    LeaderboardContent(
                        uiState = LeaderboardUiState(
                            selectedTab = LeaderboardTab.STREAKS,
                            currentUserId = testUser.id,
                            isUserOptedIn = true,
                            streakItems = listOf(leaderboardItem),
                            currentUserStreakRank = 1,
                            currentUserStreakItem = leaderboardItem,
                            isLoading = false
                        ),
                        onTabSelected = {},
                        onLoadMore = {},
                        onRefresh = {},
                        onNavigateBack = {},
                        onNavigateToAccount = { currentScreen = "ACCOUNT" }
                    )
                }
            }
        }

        // 1. Initial State: Verify Sign in button visible
        composeTestRule.onNodeWithText("🌐 SIGN IN WITH GOOGLE").assertIsDisplayed()

        // 2. Tap Sign In
        composeTestRule.onNodeWithText("🌐 SIGN IN WITH GOOGLE").performClick()
        assertTrue(authState is AuthUiState.SignedIn)

        // 3. Set Display Name
        composeTestRule.onNodeWithText("PUBLIC LEADERBOARD NAME").performTextInput("PixelPaladin")
        accountState = accountState.copy(displayNameInput = "PixelPaladin")

        // 4. Tap Opt In
        composeTestRule.onNodeWithText("🟢 JOIN LEADERBOARD (OPT IN)").performClick()
        assertTrue(accountState.showConfirmDialog)

        // 5. Confirm Opt In
        composeTestRule.onNodeWithText("CONFIRM & JOIN").performClick()
        assertTrue(accountState.isOptedIn)

        // 6. Navigate to Leaderboard
        composeTestRule.onNodeWithText("⬅️ BACK TO SETTINGS").performClick()
        assertEquals("LEADERBOARD", currentScreen)

        // 7. Verify Display Name and Rank appears on Leaderboard
        composeTestRule.onNodeWithText("PixelPaladin").assertIsDisplayed()
        composeTestRule.onNodeWithText("14d").assertIsDisplayed()
    }
}
