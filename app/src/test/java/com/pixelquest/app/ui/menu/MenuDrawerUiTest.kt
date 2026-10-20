package com.pixelquest.app.ui.menu

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixelquest.app.R
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.ui.IslamicHubContent
import com.pixelquest.app.islamic.ui.IslamicHubUiState
import com.pixelquest.app.islamic.ui.IslamicTestTags
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.islamicGraph
import com.pixelquest.app.testing.namazViewModel
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
 * The side menu end to end, built from the same pieces as MainActivity: the three-line icon on a
 * screen opens it, choosing Islamic opens the hub and closes the menu, the hub's Namaz card opens
 * the tracker, and Back closes an open menu instead of leaving the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class MenuDrawerUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun showApp(mode: ThemeMode = ThemeMode.Pixel, context: MenuContext = MenuContext()) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                val controller = rememberMenuDrawerController()
                val nav = rememberNavController()
                MenuDrawerScaffold(
                    controller = controller,
                    gesturesEnabled = true,
                    isSimpleMode = context.isSimpleMode,
                    drawerContent = {
                        MenuDrawerContent(
                            header = MenuHeaderState(name = "Aria", avatarId = "avatar_mage", level = 3, totalXp = 450, isSimpleMode = context.isSimpleMode),
                            context = context,
                            selected = null,
                            onItemClick = { item ->
                                controller.close()
                                nav.navigate(item.route)
                            }
                        )
                    }
                ) {
                    NavHost(nav, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) { HomeStandIn() }
                        islamicGraph(nav, hub = { onOpen -> IslamicHubContent(IslamicHubUiState(listOf(namazCard), loaded = true), onOpen) })
                        composable(Screen.Namaz.route) { NamazScreen(onNavigateBack = { nav.popBackStack() }, viewModel = namazViewModel()) }
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

    private val namazCard = IslamicItem("namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), Screen.Namaz.route)

    private fun islamicLabel(mode: ThemeMode) = if (mode == ThemeMode.Light) "Islamic" else "ISLAMIC"

    private fun iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen(mode: ThemeMode) {
        showApp(mode)
        composeTestRule.onNodeWithText(islamicLabel(mode)).assertIsNotDisplayed()

        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Aria").assertIsDisplayed()
        composeTestRule.onNodeWithText(islamicLabel(mode)).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()
        // The menu closed (the hub's own title says Islamic too, so check the header's name).
        composeTestRule.onNodeWithText("Aria").assertIsNotDisplayed()

        composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).assertIsDisplayed()
    }

    @Test fun pixel_iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen() = iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen(ThemeMode.Pixel)

    @Test fun comic_iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen() = iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen(ThemeMode.Comic)

    @Test fun light_iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen() = iconOpensTheMenu_islamicOpensTheHub_andNamazItsScreen(ThemeMode.Light)

    @Test
    fun back_closesTheMenu_andStaysOnTheScreen() {
        showApp()
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ISLAMIC").assertIsDisplayed()

        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ISLAMIC").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("TODAY'S DASHBOARD").assertIsDisplayed()
    }

    @Test
    fun theCloseButton_closesTheMenu() {
        showApp(ThemeMode.Comic)
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Close menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ISLAMIC").assertIsNotDisplayed()
    }

    @Test
    fun simpleMode_plainWords_noLevel() {
        showApp(context = MenuContext(isSimpleMode = true))
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Task History").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("LVL", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun namazAndDarood_haveNoEntriesOfTheirOwn() {
        showApp()
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodesWithText("NAMAZ TRACKER").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("DAROOD PARTY").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()
    }
}
