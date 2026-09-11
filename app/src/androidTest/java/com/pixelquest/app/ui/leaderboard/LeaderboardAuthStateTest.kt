package com.pixelquest.app.ui.leaderboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.remote.LeaderboardEntryDto
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardAuthState
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardContent
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LeaderboardAuthStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun leaderboard_notSignedInState_displaysLockBannerAndSignInAction() {
        var navigatedToAccount = false

        composeTestRule.setContent {
            PixelQuestTheme {
                LeaderboardContent(
                    uiState = LeaderboardUiState(
                        authState = LeaderboardAuthState.NotSignedIn,
                        streakEntries = emptyList(),
                        levelEntries = emptyList(),
                        isLoading = false
                    ),
                    onTabSelected = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onReportProfile = { _, _ -> },
                    onNavigateBack = {},
                    onNavigateToAccount = { navigatedToAccount = true }
                )
            }
        }

        // 1. Verify locked state message is prominently shown
        composeTestRule.onNodeWithText("HALL OF FAME LOCKED").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Sign in with your Google Account to view live global rankings, streaks, and top heroes across the realm."
        ).assertIsDisplayed()

        // 2. Verify Sign In button is present and functional
        composeTestRule.onNodeWithText("🔑 SIGN IN VIA ACCOUNT").assertIsDisplayed().performClick()
        assertTrue(navigatedToAccount)
    }

    @Test
    fun leaderboard_signedInNotOptedIn_displaysSpectatorModeBannerAndHeroList() {
        var navigatedToAccount = false
        var selectedTab = LeaderboardTab.TOP_STREAKS

        val sampleHeroEntries = listOf(
            LeaderboardEntryDto(
                id = "hero_1",
                displayName = "LegendaryRanger",
                currentStreak = 18,
                longestStreak = 24,
                level = 10,
                totalXp = 1050
            ),
            LeaderboardEntryDto(
                id = "hero_2",
                displayName = "ShadowMage",
                currentStreak = 14,
                longestStreak = 15,
                level = 9,
                totalXp = 920
            )
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                LeaderboardContent(
                    uiState = LeaderboardUiState(
                        authState = LeaderboardAuthState.SignedInReadOnly(userId = "spectator_user_456"),
                        selectedTab = selectedTab,
                        streakEntries = sampleHeroEntries,
                        levelEntries = emptyList(),
                        isLoading = false,
                        lastUpdatedTimestamp = "10:30 AM"
                    ),
                    onTabSelected = { selectedTab = it },
                    onRefresh = {},
                    onLoadMore = {},
                    onReportProfile = { _, _ -> },
                    onNavigateBack = {},
                    onNavigateToAccount = { navigatedToAccount = true }
                )
            }
        }

        // 1. Verify Spectator Mode banner is displayed
        composeTestRule.onNodeWithText("👁️ SPECTATOR MODE").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "You are viewing the leaderboard in read-only spectator mode. Opt in from Account Settings to appear on the leaderboard!"
        ).assertIsDisplayed()

        // 2. Verify heroes are browsable even while read-only
        composeTestRule.onNodeWithText("LegendaryRanger").assertIsDisplayed()
        composeTestRule.onNodeWithText("18 🔥").assertIsDisplayed()
        composeTestRule.onNodeWithText("ShadowMage").assertIsDisplayed()

        // 3. Verify Join Leaderboard button links to Account settings
        composeTestRule.onNodeWithText("⚔️ JOIN LEADERBOARD").assertIsDisplayed().performClick()
        assertTrue(navigatedToAccount)
    }
}
