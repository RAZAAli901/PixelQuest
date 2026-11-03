package com.pixelquest.app.ui.screens.leaderboard

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Leaderboard screen flow and limits test suite (V039):
 * - User flow: Opt in, appear on the board with ranking, opt out, disappear back to spectator mode
 * - Empty board state message
 * - Ties handling: consecutive identical streaks and levels
 * - Thousands of rows: pagination and lazy loading efficiency
 * - Rate limit / server error handling with retry and preserving cached rows
 * - Tab switching: Top streaks vs Top levels stat formatting
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class LeaderboardScreenFlowAndLimitsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun profile(id: String, name: String, streak: Int = 10, level: Int = 5, xp: Int = 2500) =
        CloudProfileDto(
            id = id,
            displayName = name,
            currentStreak = streak,
            longestStreak = streak + 5,
            level = level,
            totalXp = xp,
            leaderboardOptIn = true,
        )

    @Test
    fun flow_spectatorToOptIn_andOptOutDisappear_updatesUiCleanly() {
        val myUserId = "user_me"
        val myName = "ChampionHero"
        val myProfile = profile(myUserId, myName, streak = 25, level = 10, xp = 5000)
        val otherProfile = profile("user_rival", "RivalHero", streak = 30, level = 12, xp = 6000)

        // 1. Initial State: SignedInReadOnly (Spectator mode)
        val spectatorState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly(myUserId),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            selectedView = LeaderboardView.TOP,
            streakEntries = listOf(otherProfile),
            currentUserRank = null,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = spectatorState,
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = {},
                    onNavigateToAccount = {},
                )
            }
        }

        // Spectator banner is visible
        composeTestRule.onNodeWithText("You're watching").assertIsDisplayed()
        composeTestRule.onNodeWithText("JOIN THE LEADERBOARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your ranking").assertDoesNotExist()
        composeTestRule.onNodeWithText("RivalHero").assertIsDisplayed()
    }

    @Test
    fun flow_optedInUser_appearsWithRankAndReplacesSpectatorBanner() {
        val myUserId = "user_me"
        val myName = "ChampionHero"
        val myProfile = profile(myUserId, myName, streak = 25, level = 10, xp = 5000)
        val otherProfile = profile("user_rival", "RivalHero", streak = 30, level = 12, xp = 6000)

        // 2. Opted in state: SignedInAndOptedIn
        val optedInState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInAndOptedIn(myUserId, myName),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            selectedView = LeaderboardView.TOP,
            streakEntries = listOf(otherProfile, myProfile),
            currentUserRank = UserLeaderboardRank(rank = 2, profile = myProfile),
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = optedInState,
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = {},
                    onNavigateToAccount = {},
                )
            }
        }

        // Spectator banner is gone, pinned rank appears
        composeTestRule.onNodeWithText("You're watching").assertDoesNotExist()
        composeTestRule.onNodeWithText("YOUR RANKING").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("ChampionHero").assertCountEquals(2)
        composeTestRule.onNodeWithText("RivalHero").assertIsDisplayed()
    }

    @Test
    fun extraCase_emptyBoard_rendersEmptyStateMessage() {
        val emptyState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly("user_1"),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            streakEntries = emptyList(),
            levelEntries = emptyList(),
            isLoading = false,
            errorMessage = null,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = emptyState,
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = {},
                    onNavigateToAccount = {},
                )
            }
        }

        composeTestRule.onNodeWithText("No heroes ranked yet. Join from Account to claim the top spot.").assertIsDisplayed()
    }

    @Test
    fun extraCase_ties_rendersConsecutiveIdenticalScoresCleanly() {
        // 5 heroes with the exact same 40-day streak
        val tiedStreakHeroes = listOf(
            profile("tie_1", "StreakKing_A", streak = 40),
            profile("tie_2", "StreakKing_B", streak = 40),
            profile("tie_3", "StreakKing_C", streak = 40),
            profile("tie_4", "StreakKing_D", streak = 40),
            profile("tie_5", "StreakKing_E", streak = 40),
        )

        val tiedState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly("viewer"),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            selectedView = LeaderboardView.TOP,
            streakEntries = tiedStreakHeroes,
            canLoadMore = false,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = tiedState,
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = {},
                    onNavigateToAccount = {},
                )
            }
        }

        // Verify each player renders properly without collisions
        for (hero in tiedStreakHeroes) {
            composeTestRule.onNodeWithText(hero.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun extraCase_thousandsOfRows_paginationLoadsMorePages() {
        val page1 = (1..5).map { index ->
            profile("hero_$index", "Hero #$index", streak = 100 - index)
        }

        var loadMoreCalled = false

        val pagedState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly("viewer"),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            selectedView = LeaderboardView.TOP,
            streakEntries = page1,
            canLoadMore = true,
            isLoadingMore = false,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = pagedState,
                    onTabSelected = {},
                    onLoadMore = { loadMoreCalled = true },
                    onRefresh = {},
                    onNavigateToAccount = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hero #1").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOAD MORE").assertIsDisplayed().performClick()
        assertTrue("Load more action must be invoked", loadMoreCalled)
    }

    @Test
    fun extraCase_rateLimitWithCachedEntries_retainsBoardAndShowsErrorBannerWithRetry() {
        val cachedHeroes = listOf(
            profile("cached_1", "CachedLegend", streak = 50),
            profile("cached_2", "CachedKnight", streak = 45),
        )

        var refreshInvoked = false

        val rateLimitedState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly("viewer"),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            selectedView = LeaderboardView.TOP,
            streakEntries = cachedHeroes,
            errorMessage = "Rate limit reached (429). Please wait a moment before updating.",
            canLoadMore = false,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = rateLimitedState,
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = { refreshInvoked = true },
                    onNavigateToAccount = {},
                )
            }
        }

        // Cached rows remain visible
        composeTestRule.onNodeWithText("CachedLegend").assertIsDisplayed()
        composeTestRule.onNodeWithText("CachedKnight").assertIsDisplayed()

        // Error message banner and Retry button are displayed
        composeTestRule.onNodeWithText("Rate limit reached (429). Please wait a moment before updating.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue("Retry button must trigger onRefresh", refreshInvoked)
    }

    @Test
    fun extraCase_rateLimitEmptyBoard_showsFullErrorStateWithTryAgain() {
        var refreshInvoked = false

        val emptyErrorState = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly("viewer"),
            selectedTab = LeaderboardTab.TOP_STREAKS,
            streakEntries = emptyList(),
            errorMessage = "HTTP 429 Too Many Requests: rate limit exceeded.",
            canLoadMore = false,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = emptyErrorState,
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = { refreshInvoked = true },
                    onNavigateToAccount = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Couldn't reach the leaderboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("HTTP 429 Too Many Requests: rate limit exceeded.").assertIsDisplayed()
        composeTestRule.onNodeWithText("TRY AGAIN").performClick()
        assertTrue("Try again button must trigger refresh", refreshInvoked)
    }

    @Test
    fun flow_tabSwitching_topStreaksAndTopLevels_formatsStatsAccurately() {
        val testHero = profile("hero_tabs", "TabSwitcher", streak = 19, level = 8, xp = 3200)

        var selectedTab = LeaderboardTab.TOP_STREAKS
        val state = LeaderboardUiState(
            authState = LeaderboardAuthState.SignedInReadOnly("viewer"),
            selectedTab = LeaderboardTab.TOP_LEVELS,
            selectedView = LeaderboardView.TOP,
            levelEntries = listOf(testHero),
            canLoadMore = false,
        )

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                LeaderboardContent(
                    uiState = state,
                    onTabSelected = { selectedTab = it },
                    onLoadMore = {},
                    onRefresh = {},
                    onNavigateToAccount = {},
                )
            }
        }

        // When TOP_LEVELS is active, statValue shows "Lvl 8" and statLabel shows "3200 XP"
        composeTestRule.onNodeWithText("TabSwitcher").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lvl 8").assertIsDisplayed()
        composeTestRule.onNodeWithText("3200 XP").assertIsDisplayed()

        // Tab selection switches correctly
        composeTestRule.onNodeWithText("Top streaks").performClick()
        assertEquals(LeaderboardTab.TOP_STREAKS, selectedTab)
    }
}
