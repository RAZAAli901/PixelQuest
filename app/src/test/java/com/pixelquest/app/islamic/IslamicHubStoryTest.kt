package com.pixelquest.app.islamic

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixelquest.app.R
import com.pixelquest.app.islamic.ui.IslamicHubContent
import com.pixelquest.app.islamic.ui.IslamicHubUiState
import com.pixelquest.app.islamic.ui.IslamicTestTags
import com.pixelquest.app.quran.QuranIslamicItem
import com.pixelquest.app.quran.ui.QURAN_HOME
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.menu.MenuContext
import com.pixelquest.app.ui.menu.MenuDrawerContent
import com.pixelquest.app.ui.menu.MenuDrawerScaffold
import com.pixelquest.app.ui.menu.MenuHeaderState
import com.pixelquest.app.ui.menu.MenuTopBarAction
import com.pixelquest.app.ui.menu.rememberMenuDrawerController
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.islamicGraph
import com.pixelquest.app.ui.navigation.navigateInIslamic
import com.pixelquest.app.ui.navigation.redirect
import com.pixelquest.app.ui.screens.namaz.NamazScreen
import com.pixelquest.app.ui.screens.namaz.NamazTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Islamic hub end to end (docs/islamic-hub.md), built from the pieces MainActivity and NavGraph
 * use: the side menu's Islamic entry opens the hub; its cards open the Namaz Tracker, Darood Party
 * and the Quran; Back goes from each to the hub and out; and 1.4's notification route still opens
 * the tracker, over the hub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class IslamicHubStoryTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var nav: NavHostController

    private val cards = listOf(
        IslamicItem("namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), Screen.Namaz.route),
        IslamicItem("darood", R.string.darood_title, R.string.islamic_darood_description, IslamicIcon("📿"), Screen.DaroodParty.route),
        IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), Screen.Quran.route)
    )

    private fun showApp(mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                val menu = rememberMenuDrawerController()
                nav = rememberNavController()
                MenuDrawerScaffold(
                    controller = menu,
                    gesturesEnabled = true,
                    drawerContent = {
                        MenuDrawerContent(
                            header = MenuHeaderState(name = "Aria", avatarId = "avatar_mage"),
                            context = MenuContext(),
                            selected = null,
                            // As MainActivity opens an Islamic entry.
                            onItemClick = { item ->
                                menu.close()
                                if (Routes.isIslamic(item.route)) nav.navigateInIslamic(item.route) else nav.navigate(item.route)
                            }
                        )
                    }
                ) {
                    NavHost(nav, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) { HomeStandIn() }
                        islamicGraph(
                            nav,
                            items = listOf(com.pixelquest.app.quran.QuranIslamicItem { nav -> composable(Screen.Quran.route) { com.pixelquest.app.quran.ui.QuranHomeScreen(onNavigateBack = { nav.popBackStack() }) } }),
                            hub = { onOpen -> IslamicHubContent(IslamicHubUiState(cards, loaded = true), onOpen) }
                        ) {
                            composable(Screen.Namaz.route) { NamazScreen(onNavigateBack = { nav.popBackStack() }, viewModel = namazViewModel()) }
                            composable(Screen.DaroodParty.route) { Text("DAROOD PARTIES") }
                        }
                        redirect(Routes.LEGACY_NAMAZ, Screen.Namaz.route, nav)
                        redirect(Routes.LEGACY_DAROOD, Screen.DaroodParty.route, nav)
                    }
                }
            }
        }
    }

    @Composable
    private fun HomeStandIn() {
        Row {
            MenuTopBarAction()
            Text("TODAY'S DASHBOARD")
        }
    }

    private fun current() = composeTestRule.runOnIdle { nav.currentDestination?.route }

    private fun back() {
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }

    private fun openTheHubFromTheMenu() {
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ISLAMIC").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()
    }

    private fun openCard(id: String) {
        composeTestRule.onNodeWithTag(IslamicTestTags.card(id)).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun theMenu_opensTheHub_andNamazOpens_thenBackGoesToTheHubAndHome() {
        showApp()
        openTheHubFromTheMenu()

        openCard("namaz")
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).assertIsDisplayed()

        back()
        assertEquals(Screen.IslamicHub.route, current())
        back()
        composeTestRule.onNodeWithText("TODAY'S DASHBOARD").assertIsDisplayed()
    }

    @Test
    fun theDaroodCard_opensTheParties() {
        showApp(ThemeMode.Comic)
        openTheHubFromTheMenu()

        openCard("darood")

        composeTestRule.onNodeWithText("DAROOD PARTIES").assertIsDisplayed()
        back()
        assertEquals(Screen.IslamicHub.route, current())
    }

    @Test
    fun theQuranCard_opensItsOwnScreen_whoseBackReturnsToTheHub() {
        showApp()
        openTheHubFromTheMenu()

        openCard("quran")
        composeTestRule.onNodeWithTag(QURAN_HOME).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Screen.IslamicHub.route, current())
    }

    @Test
    fun theMenusIslamicEntry_fromInside_goesBackToTheHub() {
        showApp()
        openTheHubFromTheMenu()
        openCard("darood")

        // Choosing the menu's Islamic entry here, as MainActivity opens it, goes back to the hub
        // beneath rather than stacking a second one.
        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.IslamicHub.route) }

        assertEquals(Screen.IslamicHub.route, current())
        back()
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun aNamazReminderFrom14_opensTheTracker_overTheHub() {
        showApp()

        // MainActivity, given a reminder's tap set by 1.4: its route is the old one.
        composeTestRule.runOnIdle { nav.navigate(Routes.LEGACY_NAMAZ) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).assertIsDisplayed()
        back()
        assertEquals(Screen.IslamicHub.route, current())
        back()
        assertEquals(Screen.Home.route, current())
    }

    @Test
    fun aNamazReminderFrom15_opensTheTracker_overTheHubToo() {
        showApp()

        // MainActivity, given a reminder's tap set by this version.
        composeTestRule.runOnIdle { nav.navigateInIslamic(Screen.Namaz.route) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).assertIsDisplayed()
        back()
        assertEquals(Screen.IslamicHub.route, current())
    }
}
