package com.pixelquest.app.ui.navigation

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Islamic section's routes (docs/islamic-hub.md): one nested graph, starting at the hub. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class IslamicRoutesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var nav: NavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) { Text("home") }
                islamicGraph(nav, items = listOf(com.pixelquest.app.quran.QuranIslamicItem()), hub = { Text("hub") })
            }
        }
    }

    private fun open(route: String) = composeTestRule.runOnIdle { nav.navigate(route) }
    private fun current() = composeTestRule.runOnIdle { nav.currentDestination?.route }

    @Test
    fun theSection_startsAtTheHub() {
        open(Routes.ISLAMIC)

        assertEquals(Screen.IslamicHub.route, current())
        assertTrue(
            "the hub is inside the section's graph",
            composeTestRule.runOnIdle { nav.currentDestination!!.hierarchy.any { it.route == Routes.ISLAMIC } }
        )
    }

    @Test
    fun theHub_opensByItsOwnRoute() {
        open(Screen.IslamicHub.route)

        assertEquals("islamic/hub", current())
    }

    @Test
    fun anItemsOwnScreen_joinsTheSection() {
        open(Screen.IslamicHub.route)
        open(Screen.Quran.route)

        assertEquals("islamic/quran", current())
        composeTestRule.onNodeWithTag(com.pixelquest.app.quran.ui.QURAN_PLACEHOLDER).assertExists()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertEquals("its Back returns to the hub", Screen.IslamicHub.route, current())
    }

    @Test
    fun islamicRoutes_areTheOnesUnderIslamic() {
        assertTrue(Routes.isIslamic(Screen.IslamicHub.route))
        assertTrue(Routes.isIslamic("islamic/quran"))
        assertFalse(Routes.isIslamic(Screen.Home.route))
        assertFalse("the graph's own route isn't a screen", Routes.isIslamic(Routes.ISLAMIC))
        assertFalse(Routes.isIslamic("islamicish"))
        assertFalse(Routes.isIslamic(null))
    }
}
