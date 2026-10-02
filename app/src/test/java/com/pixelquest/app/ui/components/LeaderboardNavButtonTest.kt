package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bottom bar has a fifth, round LEADERBOARD button in the middle, in every theme.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application keeps the real app's start-up (database seeding) out of these UI tests.
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class LeaderboardNavButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun barHasFiveTabs_withLeaderboardInTheMiddle() {
        assertEquals(listOf("HOME", "TASKS", "LEADERBOARD", "STATS", "PROFILE"), bottomNavItems.map { it.title })
        assertEquals(listOf("LEADERBOARD"), bottomNavItems.filter { it.isFeatured }.map { it.title })
        assertEquals(Screen.Leaderboard.route, bottomNavItems[2].route)
    }

    private fun assertOpensLeaderboard(mode: ThemeMode) {
        var navigatedTo: String? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                PixelBottomNavBar(currentRoute = Screen.Home.route, onNavigate = { navigatedTo = it })
            }
        }
        composeTestRule.onNodeWithContentDescription("LEADERBOARD").performClick()
        assertEquals(Screen.Leaderboard.route, navigatedTo)
    }

    @Test
    fun pixelBar_leaderboardButtonOpensTheLeaderboard() = assertOpensLeaderboard(ThemeMode.Pixel)

    @Test
    fun lightBar_leaderboardButtonOpensTheLeaderboard() = assertOpensLeaderboard(ThemeMode.Light)

    @Test
    fun comicBar_leaderboardButtonOpensTheLeaderboard() = assertOpensLeaderboard(ThemeMode.Comic)

    @Test
    fun leaderboardButton_showsSelectedOnTheLeaderboard() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PixelBottomNavBar(currentRoute = Screen.Leaderboard.route, onNavigate = {})
            }
        }
        composeTestRule.onNodeWithContentDescription("LEADERBOARD").assertIsSelected()
    }
}
