package com.pixelquest.app.darood.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.menu.MenuContext
import com.pixelquest.app.ui.menu.MenuDrawerContent
import com.pixelquest.app.ui.menu.MenuDrawerScaffold
import com.pixelquest.app.ui.menu.MenuHeaderState
import com.pixelquest.app.ui.menu.MenuTopBarAction
import com.pixelquest.app.ui.menu.rememberMenuDrawerController
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.islamicGraph
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Darood Party from the side menu, built from the same pieces as MainActivity: the Islamic entry
 * opens the hub, whose Darood card opens its screen. That asks a signed-out player to sign in
 * (opening the Account screen) and shows a signed-in one their parties. With its flag off, the card
 * is gone; a waiting invitation puts a dot on the Islamic entry.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class DaroodMenuUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val auth = FakeAuthRepository()
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(
        androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>(),
        com.pixelquest.app.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()
    private val server = com.pixelquest.app.testing.FakeDaroodServer(me = "aria-id")
    private val repository = com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl(
        server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") }
    )
    private val settings = com.pixelquest.app.testing.FakeSettingsRepository()
    private val viewModels = com.pixelquest.app.testing.TestViewModels()
    private val hub by lazy {
        viewModels.keep(com.pixelquest.app.islamic.ui.IslamicHubViewModel::class.java) {
            com.pixelquest.app.islamic.ui.IslamicHubViewModel(
                com.pixelquest.app.islamic.IslamicRegistry(setOf(DaroodIslamicItem(settings, repository))),
                settings
            )
        }
    }

    @org.junit.After
    fun closeDb() {
        // The hub reads the parties from the database: stop it before the database closes.
        viewModels.clear()
        db.closeAfterWrites()
    }

    private fun showApp(mode: ThemeMode = ThemeMode.Pixel, context: MenuContext = MenuContext()) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode, darkTheme = mode == ThemeMode.Pixel) {
                val controller = rememberMenuDrawerController()
                val nav = rememberNavController()
                MenuDrawerScaffold(
                    controller = controller,
                    gesturesEnabled = true,
                    isSimpleMode = context.isSimpleMode,
                    drawerContent = {
                        MenuDrawerContent(
                            header = MenuHeaderState(name = "Aria", avatarId = "avatar_mage", isSimpleMode = context.isSimpleMode),
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
                        islamicGraph(nav, hub = { onOpen -> com.pixelquest.app.islamic.ui.IslamicHubScreen(onOpen, viewModel = hub) })
                        composable(Screen.DaroodParty.route) {
                            DaroodPartyScreen(
                                onNavigateBack = { nav.popBackStack() },
                                onSignIn = { nav.navigate(Screen.Account.route) },
                                onOpenParty = {},
                                viewModel = com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel(auth, repository)
                            )
                        }
                        composable(Screen.Account.route) { Text("ACCOUNT SCREEN") }
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

    /** The list comes from the database, which answers on its own thread. */
    private fun waitForTag(tag: String) = composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openTheHub(islamicLabel: String) {
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(islamicLabel).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(com.pixelquest.app.islamic.ui.IslamicTestTags.HUB).assertIsDisplayed()
    }

    private fun openDaroodFromTheMenu(islamicLabel: String) {
        openTheHub(islamicLabel)
        waitForTag(com.pixelquest.app.islamic.ui.IslamicTestTags.card("darood"))
        composeTestRule.onNodeWithTag(com.pixelquest.app.islamic.ui.IslamicTestTags.card("darood")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(DaroodTestTags.PARTY_SCREEN).assertIsDisplayed()
    }

    @Test
    fun signedOut_theEntryOpensTheSignInPrompt_whichOpensTheAccountScreen() {
        showApp(ThemeMode.Pixel)
        openDaroodFromTheMenu("ISLAMIC")

        waitForTag(DaroodTestTags.SIGNED_OUT)
        composeTestRule.onNodeWithTag(DaroodTestTags.SIGNED_OUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.SIGN_IN).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ACCOUNT SCREEN").assertIsDisplayed()
    }

    @Test
    fun signedIn_theEntryOpensThePlaceholder_inComic() {
        auth.userFlow.value = AuthUser(id = "aria-id", email = null, displayName = null)
        showApp(ThemeMode.Comic)
        openDaroodFromTheMenu("ISLAMIC")

        waitForTag(DaroodTestTags.EMPTY)
        composeTestRule.onNodeWithTag(DaroodTestTags.EMPTY).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Sign in to join a party").fetchSemanticsNodes().size)
    }

    @Test
    fun inLight_theEntryReadsAsWritten() {
        showApp(ThemeMode.Light)
        openDaroodFromTheMenu("Islamic")
    }

    @Test
    fun withItsFlagOff_theCardIsGone() {
        settings.daroodAvailable.value = false
        showApp()
        openTheHub("ISLAMIC")
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(com.pixelquest.app.islamic.ui.IslamicTestTags.card("darood"))).fetchSemanticsNodes().size)
    }

    @Test
    fun aWaitingInvitation_putsADotOnTheEntry() {
        showApp(context = MenuContext(daroodInviteWaiting = true))
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        // The row is one button to TalkBack, so the dot is found in the unmerged tree, and its words
        // in the row's own description.
        composeTestRule.onNodeWithTag(com.pixelquest.app.ui.menu.MenuDot, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Something waits for you here", substring = true).assertIsDisplayed()
    }

    @Test
    fun noInvitation_noDot() {
        showApp()
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(com.pixelquest.app.ui.menu.MenuDot), useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
