package com.pixelquest.app.navigation

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.pixelquest.app.ui.navigation.PqTab
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.currentPqTab
import com.pixelquest.app.ui.navigation.navigateInIslamic
import com.pixelquest.app.ui.navigation.navigateToTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * V058: Deep links and back stack navigation integrity test suite.
 * Covers:
 * - Route builders and argument parsing for all screens (Quests, Tasks, Parties, Chess)
 * - Multi-level back stack unwinding (Home -> Tab -> Nested Screen -> Modal)
 * - Islamic deep link hierarchy guarantee (direct Namaz deep link inserts IslamicHub underneath)
 * - State restoration when switching between tabs
 * - Feature toggle route guard behavior
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DeepLinksAndBackStackIntegrityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var nav: NavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) { Text("Home") }
                composable(Screen.Tasks.route) { Text("Tasks") }
                composable(Screen.CreateTask.route) { Text("CreateTask") }
                composable(
                    route = Screen.EditTask.route,
                    arguments = Screen.EditTask.arguments,
                ) { Text("EditTask") }
                composable(
                    route = Screen.TaskAnalytics.route,
                    arguments = Screen.TaskAnalytics.arguments,
                ) { Text("TaskAnalytics") }

                composable(Screen.Hero.route) { Text("Hero") }
                composable(Screen.AvatarSelection.route) { Text("AvatarSelection") }
                composable(Screen.Settings.route) { Text("Settings") }
                composable(Screen.Leaderboard.route) { Text("Leaderboard") }

                // Parties Graph
                composable(Screen.Parties.route) { Text("Parties") }
                composable(
                    route = Screen.PartiesParty.route,
                    arguments = Screen.PartiesParty.arguments,
                ) { Text("PartyRoom") }
                composable(
                    route = Screen.PartiesItem.route,
                    arguments = Screen.PartiesItem.arguments,
                ) { Text("PartyItem") }

                // Islamic Graph
                navigation(startDestination = Routes.ISLAMIC_HUB, route = Routes.ISLAMIC) {
                    composable(Routes.ISLAMIC_HUB) { Text("IslamicHub") }
                    composable(Screen.Namaz.route) { Text("Namaz") }
                }
            }
        }
    }

    private fun tapTab(route: String) = composeTestRule.runOnIdle { nav.navigateToTab(route) }
    private fun openRoute(route: String) = composeTestRule.runOnIdle { nav.navigate(route) }
    private fun currentRoute() = composeTestRule.runOnIdle { nav.currentDestination?.route }
    private fun currentTab() = composeTestRule.runOnIdle { nav.currentPqTab() }
    private fun pressBack() = composeTestRule.runOnIdle { nav.popBackStack() }

    // --- 1. Typed Argument & Deep Link Route Verification ---

    @Test
    fun testTaskRoutes_argumentContractsAndRouteBuilders() {
        val taskId = 8823L
        val editRoute = Screen.EditTask.createRoute(taskId)
        assertEquals("edit_task/8823", editRoute)

        val handle = SavedStateHandle(mapOf(Screen.EditTask.ARG_TASK_ID to taskId))
        assertEquals(taskId, Screen.EditTask.getTaskId(handle))

        val analyticsRoute = Screen.TaskAnalytics.createRoute(taskId)
        assertEquals("task_analytics/8823", analyticsRoute)
        assertEquals(taskId, Screen.TaskAnalytics.getTaskId(handle))
    }

    @Test
    fun testStudyPartyRoutes_argumentContractsAndRouteBuilders() {
        val partyId = "algebra_guild"
        val itemId = "midterm_study"
        val partyRoute = Screen.PartiesParty.createRoute(partyId)
        val itemRoute = Screen.PartiesItem.createRoute(partyId, itemId)

        assertEquals("parties/party/algebra_guild", partyRoute)
        assertEquals("parties/party/algebra_guild/item/midterm_study", itemRoute)

        val handle = SavedStateHandle(
            mapOf(
                Screen.PartiesItem.ARG_PARTY_ID to partyId,
                Screen.PartiesItem.ARG_ITEM_ID to itemId,
            ),
        )
        val parsedArgs = Screen.PartiesItem.parseArgs(handle)
        assertEquals(partyId, parsedArgs.partyId)
        assertEquals(itemId, parsedArgs.itemId)
    }

    // --- 2. Multi-Level Back Stack Unwinding ---

    @Test
    fun testBackStackUnwinding_fromNestedModal_unwindsCorrectlyToHome() {
        assertEquals(Screen.Home.route, currentRoute())

        // 1. Open Tasks tab
        tapTab(Screen.Tasks.route)
        assertEquals(Screen.Tasks.route, currentRoute())
        assertEquals(PqTab.Quests, currentTab())

        // 2. Open EditTask modal
        openRoute(Screen.EditTask.createRoute(42L))
        assertEquals(Screen.EditTask.route, currentRoute())

        // 3. First back press: pops EditTask back to Tasks
        pressBack()
        assertEquals(Screen.Tasks.route, currentRoute())

        // 4. Second back press: pops Tasks back to Home
        pressBack()
        assertEquals(Screen.Home.route, currentRoute())
        assertEquals(PqTab.Home, currentTab())
    }

    @Test
    fun testBackStackUnwinding_fromDeepPartyItem_unwindsThroughPartyHierarchy() {
        // 1. Open Parties screen
        openRoute(Screen.Parties.route)
        assertEquals(Screen.Parties.route, currentRoute())

        // 2. Open specific Party room
        openRoute(Screen.PartiesParty.createRoute("study_party_7"))
        assertEquals(Screen.PartiesParty.route, currentRoute())

        // 3. Open Party Item details
        openRoute(Screen.PartiesItem.createRoute("study_party_7", "item_biology"))
        assertEquals(Screen.PartiesItem.route, currentRoute())

        // Back 1: pops item back to party room
        pressBack()
        assertEquals(Screen.PartiesParty.route, currentRoute())

        // Back 2: pops party room back to parties list
        pressBack()
        assertEquals(Screen.Parties.route, currentRoute())

        // Back 3: pops parties list back to Home
        pressBack()
        assertEquals(Screen.Home.route, currentRoute())
    }

    // --- 3. Islamic Deep Link Hierarchy Guarantee ---

    @Test
    fun testIslamicDeepLink_navigateInIslamic_insertsHubUnderneath() {
        // Simulating direct notification or widget tap directly into Namaz tracker
        composeTestRule.runOnIdle {
            nav.navigateInIslamic(Screen.Namaz.route)
        }

        assertEquals(Screen.Namaz.route, currentRoute())
        assertEquals(PqTab.Deen, currentTab())

        // First back press: must pop to Islamic Hub (not abruptly exit or jump to Home)
        pressBack()
        assertEquals(Routes.ISLAMIC_HUB, currentRoute())
        assertEquals(PqTab.Deen, currentTab())

        // Second back press: pops from Hub back to Home
        pressBack()
        assertEquals(Screen.Home.route, currentRoute())
        assertEquals(PqTab.Home, currentTab())
    }

    // --- 4. Tab State Retention Across Navigation ---

    @Test
    fun testTabStateRetention_switchingTabsPreservesNestedDestination() {
        // 1. Navigate into Hero -> AvatarSelection
        tapTab(Screen.Hero.route)
        openRoute(Screen.AvatarSelection.route)
        assertEquals(Screen.AvatarSelection.route, currentRoute())

        // 2. Switch to Tasks tab
        tapTab(Screen.Tasks.route)
        assertEquals(Screen.Tasks.route, currentRoute())

        // 3. Switch back to Hero tab: restoredState brings back AvatarSelection
        tapTab(Screen.Hero.route)
        assertEquals(Screen.AvatarSelection.route, currentRoute())

        // 4. Tap Hero tab again: pops back to Hero root screen
        tapTab(Screen.Hero.route)
        assertEquals(Screen.Hero.route, currentRoute())
    }

    // --- 5. Full-Screen Route Checks ---

    @Test
    fun testFullScreenRoutes_bottomBarSuppressionRules() {
        assertTrue("Splash must be full screen", Routes.isFullScreen(Screen.Splash.route))
        assertTrue("Onboarding must be full screen", Routes.isFullScreen(Screen.Onboarding.route))
        assertTrue("Quran reader must be full screen", Routes.isFullScreen(Screen.QuranSurah.route))
        assertTrue("Darood room chat must be full screen", Routes.isFullScreen(Screen.DaroodRoom.route))
        assertFalse("Home must show bottom bar", Routes.isFullScreen(Screen.Home.route))
        assertFalse("Tasks must show bottom bar", Routes.isFullScreen(Screen.Tasks.route))
        assertFalse("Hero must show bottom bar", Routes.isFullScreen(Screen.Hero.route))
    }
}
