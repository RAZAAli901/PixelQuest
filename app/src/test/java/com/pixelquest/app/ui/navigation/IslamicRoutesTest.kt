package com.pixelquest.app.ui.navigation

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
                islamicGraph(nav, items = listOf(com.pixelquest.app.quran.QuranIslamicItem { nav -> composable(Screen.Quran.route) { com.pixelquest.app.quran.ui.QuranHomeScreen(onNavigateBack = { nav.popBackStack() }) } }), hub = { Text("hub") }) {
                    composable(Screen.Namaz.route) { Text("namaz screen") }
                    composable(Screen.DaroodParty.route) { Text("darood screen") }
                    composable(Screen.DaroodPartyChat.route) { Text("chat") }
                }
                redirect(Routes.LEGACY_NAMAZ, Screen.Namaz.route, nav)
                redirect(Routes.LEGACY_DAROOD, Screen.DaroodParty.route, nav)
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
        composeTestRule.onNodeWithTag(com.pixelquest.app.quran.ui.QURAN_HOME).assertExists()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertEquals("its Back returns to the hub", Screen.IslamicHub.route, current())
    }

    @Test
    fun namazAndDarood_liveInsideTheSection() {
        for (route in listOf(Screen.Namaz.route, Screen.DaroodParty.route, Screen.DaroodPartyChat.createRoute("p1"))) {
            assertTrue(route, Routes.isIslamic(route))
        }
        open(Screen.DaroodPartyChat.createRoute("p1"))
        assertTrue(composeTestRule.runOnIdle { nav.currentDestination!!.hierarchy.any { it.route == Routes.ISLAMIC } })
    }

    private fun back() = composeTestRule.runOnIdle { nav.popBackStack() }

    @Test
    fun fromHome_anIslamicScreenOpensWithTheHubBeneath() {
        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.Namaz.route) }

        assertEquals(Screen.Namaz.route, current())
        back()
        assertEquals("Back goes to the hub", Screen.IslamicHub.route, current())
        back()
        assertEquals("then to Home", Screen.Home.route, current())
    }

    @Test
    fun fromInsideTheSection_itIsAnOrdinaryStep() {
        open(Screen.IslamicHub.route)
        open(Screen.DaroodParty.route)

        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.Namaz.route) }

        back()
        assertEquals("no second hub was added", Screen.DaroodParty.route, current())
        back()
        assertEquals(Screen.IslamicHub.route, current())
    }

    @Test
    fun theOldNamazRoute_opensTheTracker_andLeavesNothingBehind() {
        open(Routes.LEGACY_NAMAZ)
        composeTestRule.waitForIdle()

        assertEquals(Screen.Namaz.route, current())
        composeTestRule.onNodeWithText("namaz screen").assertExists()
        assertFalse(
            "the redirect isn't on the back stack",
            composeTestRule.runOnIdle { nav.currentBackStack.value.any { it.destination.route == Routes.LEGACY_NAMAZ } }
        )
    }

    @Test
    fun theOldDaroodRoute_opensTheParties() {
        open(Routes.LEGACY_DAROOD)
        composeTestRule.waitForIdle()

        assertEquals(Screen.DaroodParty.route, current())
    }

    @Test
    fun aNotificationsOldRoute_opensTheTrackerOverTheHub() {
        open(Routes.LEGACY_NAMAZ)
        composeTestRule.waitForIdle()

        back()
        assertEquals(Screen.IslamicHub.route, current())
        back()
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun theHub_whenOpenBeneath_isGoneBackTo_notOpenedAgain() {
        open(Screen.IslamicHub.route)
        open(Screen.DaroodParty.route)
        open(Screen.DaroodPartyChat.createRoute("p1"))

        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.IslamicHub.route) }

        assertEquals(Screen.IslamicHub.route, current())
        back()
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun theHub_fromOutside_opensOnce() {
        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.IslamicHub.route) }

        assertEquals(Screen.IslamicHub.route, current())
        back()
        assertEquals(Screen.Home.route, current())
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
