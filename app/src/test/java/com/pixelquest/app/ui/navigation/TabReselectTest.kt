package com.pixelquest.app.ui.navigation

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bottom bar's tabs. Tapping the tab you're in goes back to its first screen: tapping HOME on
 * the Account screen (opened from Today's AI card) used to do nothing, because the tab's saved state,
 * which was that same Account screen, was restored. Switching tabs still keeps each tab's place, and
 * the tab lit is the one the screen was opened from.
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
                for (route in listOf(Screen.Home.route, Screen.Tasks.route, Screen.Hero.route, Screen.Leaderboard.route, Screen.Account.route, "detail")) {
                    composable(route) { Text(route) }
                }
                // Deen's back stack is the Islamic section's graph, as in NavGraph.
                navigation(startDestination = Routes.ISLAMIC_HUB, route = Routes.ISLAMIC) {
                    composable(Routes.ISLAMIC_HUB) { Text("hub") }
                    composable(Screen.Namaz.route) { Text("namaz") }
                }
            }
        }
    }

    private fun tap(route: String) = composeTestRule.runOnIdle { nav.navigateToTab(route) }
    private fun open(route: String) = composeTestRule.runOnIdle { nav.navigate(route) }
    private fun current() = composeTestRule.runOnIdle { nav.currentDestination?.route }
    private fun tab() = composeTestRule.runOnIdle { nav.currentPqTab() }
    private fun back() = composeTestRule.runOnIdle { nav.popBackStack() }

    @Test
    fun home_fromAScreenOpenedOnToday_goesBackToToday() {
        open(Screen.Account.route)
        tap(Screen.Home.route)
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun home_fromAnotherTab_goesHome() {
        tap(Screen.Hero.route)
        tap(Screen.Home.route)
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun theTabYoureIn_goesBackToItsFirstScreen() {
        tap(Screen.Hero.route)
        open("detail")
        tap(Screen.Hero.route)
        assertEquals(Screen.Hero.route, current())
    }

    @Test
    fun deen_reselected_goesBackToTheHub_notItsGraph() {
        tap(Routes.ISLAMIC)
        assertEquals(Routes.ISLAMIC_HUB, current())
        open(Screen.Namaz.route)
        tap(Routes.ISLAMIC)
        assertEquals(Routes.ISLAMIC_HUB, current())
    }

    @Test
    fun switchingTabs_keepsEachTabsPlace() {
        tap(Screen.Hero.route)
        open("detail")
        tap(Screen.Tasks.route)
        assertEquals(Screen.Tasks.route, current())
        tap(Screen.Hero.route)
        assertEquals("Coming back to Hero shows where you were", "detail", current())
    }

    @Test
    fun theTabLit_isTheOneTheScreenWasOpenedFrom() {
        assertEquals(PqTab.Home, tab())
        open(Screen.Account.route)
        assertEquals("opened from Home", PqTab.Home, tab())

        tap(Screen.Hero.route)
        open(Screen.Account.route)
        assertEquals(PqTab.Hero, tab())

        tap(Routes.ISLAMIC)
        open(Screen.Namaz.route)
        assertEquals(PqTab.Deen, tab())

        tap(Screen.Leaderboard.route)
        assertNull("the trophy's screen lights no tab", tab())
    }

    @Test
    fun anIslamicScreenFromAnotherTab_opensInDeen_overTheHub() {
        tap(Screen.Tasks.route)
        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.Namaz.route) }

        assertEquals(Screen.Namaz.route, current())
        assertEquals(PqTab.Deen, tab())
        back()
        assertEquals(Routes.ISLAMIC_HUB, current())
        back()
        assertEquals(Screen.Home.route, current())
    }
}
