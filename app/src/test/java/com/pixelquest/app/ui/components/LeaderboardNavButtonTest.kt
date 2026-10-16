package com.pixelquest.app.ui.components

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.getBoundsInRoot
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
 * The bottom bar has a round LEADERBOARD button in the middle, in every theme, centred in a slot
 * as wide as the other tabs.
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

    private fun assertCentred(mode: ThemeMode) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                PixelBottomNavBar(currentRoute = Screen.Home.route, onNavigate = {})
            }
        }
        // Between the outer tabs' edges: the Comic panel's drop shadow takes room on its right.
        val first = composeTestRule.onNodeWithContentDescription("HOME").getBoundsInRoot()
        val last = composeTestRule.onNodeWithContentDescription("PROFILE").getBoundsInRoot()
        val button = composeTestRule.onNodeWithContentDescription("LEADERBOARD").getBoundsInRoot()
        val offCentre = ((button.left + button.right) / 2 - (first.left + last.right) / 2).value
        // Comic used to draw a divider on its right side only, which pushed it off the middle.
        assertTrue("$mode: ${offCentre}dp off the middle", kotlin.math.abs(offCentre) <= 1f)
    }

    @Test
    fun pixelBar_leaderboardButtonIsCentred() = assertCentred(ThemeMode.Pixel)

    @Test
    fun lightBar_leaderboardButtonIsCentred() = assertCentred(ThemeMode.Light)

    @Test
    fun comicBar_leaderboardButtonIsCentred() = assertCentred(ThemeMode.Comic)
}
