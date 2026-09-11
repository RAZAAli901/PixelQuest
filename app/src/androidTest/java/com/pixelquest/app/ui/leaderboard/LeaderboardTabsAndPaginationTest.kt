package com.pixelquest.app.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardContent
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardItemUiModel
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardTab
import com.pixelquest.app.ui.screens.leaderboard.LeaderboardUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LeaderboardTabsAndPaginationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun leaderboard_tabSwitchingAndPagination() {
        var selectedTab by mutableStateOf(LeaderboardTab.STREAKS)
        var streakItems by mutableStateOf(
            listOf(
                LeaderboardItemUiModel(
                    rank = 1,
                    id = "p1",
                    displayName = "StreakKing",
                    score = 25,
                    secondaryScore = 5,
                    scoreLabel = "25d",
                    secondaryLabel = "Lvl 5"
                ),
                LeaderboardItemUiModel(
                    rank = 2,
                    id = "p2",
                    displayName = "FlameHero",
                    score = 20,
                    secondaryScore = 4,
                    scoreLabel = "20d",
                    secondaryLabel = "Lvl 4"
                )
            )
        )

        val levelItems = listOf(
            LeaderboardItemUiModel(
                rank = 1,
                id = "p3",
                displayName = "MaxLevelGrandmaster",
                score = 50,
                secondaryScore = 5000,
                scoreLabel = "Lvl 50",
                secondaryLabel = "5000 XP"
            ),
            LeaderboardItemUiModel(
                rank = 2,
                id = "p4",
                displayName = "Archmage",
                score = 42,
                secondaryScore = 4200,
                scoreLabel = "Lvl 42",
                secondaryLabel = "4200 XP"
            )
        )

        var loadMoreTriggeredCount = 0

        composeTestRule.setContent {
            PixelQuestTheme {
                val currentItems = if (selectedTab == LeaderboardTab.STREAKS) streakItems else levelItems
                LeaderboardContent(
                    uiState = LeaderboardUiState(
                        selectedTab = selectedTab,
                        streakItems = if (selectedTab == LeaderboardTab.STREAKS) currentItems else emptyList(),
                        levelItems = if (selectedTab == LeaderboardTab.LEVELS) currentItems else emptyList(),
                        isLoading = false,
                        hasMoreStreaks = true,
                        hasMoreLevels = false
                    ),
                    onTabSelected = { tab ->
                        selectedTab = tab
                    },
                    onLoadMore = {
                        loadMoreTriggeredCount++
                        if (selectedTab == LeaderboardTab.STREAKS) {
                            streakItems = streakItems + LeaderboardItemUiModel(
                                rank = 3,
                                id = "p5_paged",
                                displayName = "PagedWarrior",
                                score = 15,
                                secondaryScore = 3,
                                scoreLabel = "15d",
                                secondaryLabel = "Lvl 3"
                            )
                        }
                    },
                    onRefresh = {},
                    onNavigateBack = {},
                    onNavigateToAccount = {}
                )
            }
        }

        // 1. Initially on Streaks Tab: Verify streak items visible
        composeTestRule.onNodeWithText("StreakKing").assertIsDisplayed()
        composeTestRule.onNodeWithText("25d").assertIsDisplayed()
        composeTestRule.onNodeWithText("FlameHero").assertIsDisplayed()

        // 2. Switch to Levels Tab
        composeTestRule.onNodeWithText("⚔️ TOP LEVELS").performClick()
        assertEquals(LeaderboardTab.LEVELS, selectedTab)

        // 3. Verify level items visible
        composeTestRule.onNodeWithText("MaxLevelGrandmaster").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lvl 50").assertIsDisplayed()
        composeTestRule.onNodeWithText("Archmage").assertIsDisplayed()

        // 4. Switch back to Streaks Tab
        composeTestRule.onNodeWithText("🔥 TOP STREAKS").performClick()
        assertEquals(LeaderboardTab.STREAKS, selectedTab)

        // 5. Test Pagination loadMore trigger
        composeTestRule.runOnIdle {
            // Trigger load more simulation
            streakItems = streakItems + LeaderboardItemUiModel(
                rank = 3,
                id = "p5_paged",
                displayName = "PagedWarrior",
                score = 15,
                secondaryScore = 3,
                scoreLabel = "15d",
                secondaryLabel = "Lvl 3"
            )
        }

        // 6. Verify paged item rendered
        composeTestRule.onNodeWithText("PagedWarrior").assertIsDisplayed()
        composeTestRule.onNodeWithText("15d").assertIsDisplayed()
    }
}
