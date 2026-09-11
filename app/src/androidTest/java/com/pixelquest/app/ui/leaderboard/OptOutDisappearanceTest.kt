package com.pixelquest.app.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.PixelAuthUser
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardContent
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardItemUiModel
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OptOutDisappearanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun optOut_disappearsFromLeaderboard() {
        val testUser = PixelAuthUser(
            id = "hero-optout-123",
            email = "hero@pixelquest.test",
            displayName = "ChampionHero"
        )

        var accountState by mutableStateOf(
            AccountUiState(
                isOptedIn = true,
                displayNameInput = "ChampionHero"
            )
        )
        var currentScreen by mutableStateOf("LEADERBOARD")

        val initialUserItem = LeaderboardItemUiModel(
            rank = 1,
            id = testUser.id,
            displayName = "ChampionHero",
            score = 20,
            secondaryScore = 20,
            scoreLabel = "20d",
            secondaryLabel = "Lvl 6",
            isCurrentUser = true,
            isOptedIn = true
        )

        val competitorItem = LeaderboardItemUiModel(
            rank = 2,
            id = "other-player-456",
            displayName = "RetroGamer",
            score = 15,
            secondaryScore = 15,
            scoreLabel = "15d",
            secondaryLabel = "Lvl 4",
            isCurrentUser = false,
            isOptedIn = true
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                if (currentScreen == "LEADERBOARD") {
                    // When opted out, user item is filtered out and spectator banner is active
                    val displayedItems = if (accountState.isOptedIn) {
                        listOf(initialUserItem, competitorItem)
                    } else {
                        listOf(competitorItem.copy(rank = 1))
                    }

                    LeaderboardContent(
                        uiState = LeaderboardUiState(
                            selectedTab = LeaderboardTab.STREAKS,
                            currentUserId = testUser.id,
                            isUserOptedIn = accountState.isOptedIn,
                            streakItems = displayedItems,
                            currentUserStreakRank = if (accountState.isOptedIn) 1 else null,
                            currentUserStreakItem = if (accountState.isOptedIn) initialUserItem else null,
                            isLoading = false
                        ),
                        onTabSelected = {},
                        onLoadMore = {},
                        onRefresh = {},
                        onNavigateBack = {},
                        onNavigateToAccount = { currentScreen = "ACCOUNT" }
                    )
                } else {
                    AccountContent(
                        authState = AuthUiState.SignedIn(testUser),
                        accountState = accountState,
                        onOptInToggle = { target ->
                            if (!target) {
                                accountState = accountState.copy(showOptOutConfirmDialog = true)
                            }
                        },
                        onConfirmOptOut = {
                            accountState = accountState.copy(
                                isOptedIn = false,
                                showOptOutConfirmDialog = false,
                                showOptOutSuccessNotice = true
                            )
                        },
                        onDismissOptOutDialog = {
                            accountState = accountState.copy(showOptOutConfirmDialog = false)
                        },
                        onDismissOptOutNotice = {
                            accountState = accountState.copy(showOptOutSuccessNotice = false)
                            currentScreen = "LEADERBOARD"
                        },
                        onNavigateBack = {
                            currentScreen = "LEADERBOARD"
                        }
                    )
                }
            }
        }

        // 1. Initial State: ChampionHero is visible on Leaderboard
        composeTestRule.onNodeWithText("ChampionHero").assertIsDisplayed()
        composeTestRule.onNodeWithText("RetroGamer").assertIsDisplayed()

        // 2. Open AccountScreen
        composeTestRule.onNodeWithText("⚙️ SETTINGS / OPT-OUT").performClick()

        // 3. Verify AccountScreen shows opted in status and Leave button
        composeTestRule.onNodeWithText("STATUS: ACTIVE (OPTED IN)").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔴 LEAVE LEADERBOARD (OPT OUT)").performClick()

        // 4. Lightweight confirmation dialog appears
        composeTestRule.onNodeWithText("LEAVE LEADERBOARD?").assertIsDisplayed()
        composeTestRule.onNodeWithText("LEAVE").performClick()

        // 5. Post-opt-out notice appears
        composeTestRule.onNodeWithText("LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("You've left the leaderboard. Your rank and display name have been removed from public rankings.").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()

        // 6. Returned to Leaderboard: ChampionHero has disappeared; Spectator mode is active!
        composeTestRule.onNodeWithText("👁️ SPECTATOR MODE").assertIsDisplayed()
        composeTestRule.onNodeWithText("RetroGamer").assertIsDisplayed()
        assertFalse(accountState.isOptedIn)
    }
}
