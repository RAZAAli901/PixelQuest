package com.pixelquest.app.ui.screens.leaderboard

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.remote.model.CloudProfileDto
import com.pixelquest.app.data.repository.RankedProfile
import com.pixelquest.app.data.repository.UserLeaderboardRank
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opened from the bottom bar, the leaderboard shows the heroes just above and below you.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application keeps the real app's start-up (database seeding) out of these UI tests.
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class LeaderboardAroundYouUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun hero(id: String, streak: Int) =
        CloudProfileDto(id = id, displayName = id, currentStreak = streak, longestStreak = streak, leaderboardOptIn = true)

    private val you = hero("You_Hero", 12)
    private val around = listOf(
        RankedProfile(5, hero("Above_Two", 14)),
        RankedProfile(6, hero("Above_One", 13)),
        RankedProfile(7, you),
        RankedProfile(8, hero("Below_One", 11)),
        RankedProfile(9, hero("Below_Two", 10))
    )

    private fun show(view: LeaderboardView, onViewSelected: (LeaderboardView) -> Unit = {}) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                LeaderboardContent(
                    uiState = LeaderboardUiState(
                        authState = LeaderboardAuthState.SignedInAndOptedIn(userId = you.id, displayName = you.displayName),
                        streakEntries = listOf(hero("Champion", 99)),
                        currentUserRank = UserLeaderboardRank(7, you),
                        selectedView = view,
                        aroundYouEntries = around,
                        canLoadMore = false
                    ),
                    onTabSelected = {},
                    onLoadMore = {},
                    onRefresh = {},
                    onViewSelected = onViewSelected,
                    onNavigateToAccount = {}
                )
            }
        }
    }

    @Test
    fun aroundYou_listsTheHeroesAboveAndBelow() {
        show(LeaderboardView.AROUND_YOU)

        composeTestRule.onNodeWithText("HEROES ABOVE AND BELOW YOU").assertExists()
        listOf("Above_Two", "Above_One", "You_Hero", "Below_One", "Below_Two").forEach {
            composeTestRule.onNodeWithText(it, substring = true).assertExists()
        }
        composeTestRule.onNodeWithText("Champion").assertDoesNotExist()
    }

    @Test
    fun asATab_thereIsNoBackArrow() {
        show(LeaderboardView.AROUND_YOU)

        composeTestRule.onNodeWithText("◀").assertDoesNotExist()
    }

    @Test
    fun switchingToTopHeroes_isReported() {
        var selected: LeaderboardView? = null
        show(LeaderboardView.AROUND_YOU) { selected = it }

        composeTestRule.onNodeWithText("🏆 TOP HEROES").performClick()
        assertEquals(LeaderboardView.TOP, selected)
    }

    @Test
    fun topHeroesView_showsTheTopList() {
        show(LeaderboardView.TOP)

        composeTestRule.onNodeWithText("Champion", substring = true).assertExists()
    }
}
