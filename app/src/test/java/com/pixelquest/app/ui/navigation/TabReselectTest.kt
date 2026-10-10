package com.pixelquest.app.ui.navigation

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bottom bar's tabs. Tapping the tab you're in goes back to its first screen: tapping HOME on
 * the Account screen (opened from Today's AI card) used to do nothing, because the tab's saved state,
 * which was that same Account screen, was restored. Switching tabs still keeps each tab's place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TabReselectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var nav: NavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Screen.Home.route) {
                for (route in listOf(Screen.Home.route, Screen.Tasks.route, Screen.Stats.route, Screen.Profile.route, Screen.Account.route, "detail")) {
                    composable(route) { Text(route) }
                }
            }
        }
    }

    private fun tap(route: String) = composeTestRule.runOnIdle { nav.navigateToTab(route) }
    private fun open(route: String) = composeTestRule.runOnIdle { nav.navigate(route) }
    private fun current() = composeTestRule.runOnIdle { nav.currentDestination?.route }

    @Test
    fun home_fromAScreenOpenedOnToday_goesBackToToday() {
        open(Screen.Account.route)
        tap(Screen.Home.route)
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun home_fromAnotherTab_goesHome() {
        tap(Screen.Stats.route)
        tap(Screen.Home.route)
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun theTabYoureIn_goesBackToItsFirstScreen() {
        tap(Screen.Profile.route)
        open("detail")
        tap(Screen.Profile.route)
        assertEquals(Screen.Profile.route, current())
    }

    @Test
    fun switchingTabs_keepsEachTabsPlace() {
        tap(Screen.Profile.route)
        open("detail")
        tap(Screen.Stats.route)
        assertEquals(Screen.Stats.route, current())
        tap(Screen.Profile.route)
        assertEquals("Coming back to Profile shows where you were", "detail", current())
    }
}
