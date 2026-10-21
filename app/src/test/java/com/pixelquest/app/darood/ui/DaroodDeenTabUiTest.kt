package com.pixelquest.app.darood.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
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
import com.pixelquest.app.ui.navigation.PqAppFrame
import com.pixelquest.app.ui.navigation.PqNavTags
import com.pixelquest.app.ui.navigation.PqRootState
import com.pixelquest.app.ui.navigation.PqTab
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
 * Darood Party from the Deen tab, built from the same pieces as MainActivity: the tab opens the hub,
 * whose Darood card opens the parties. That asks a signed-out player to sign in (opening the Account
 * screen) and shows a signed-in one their parties. With its flag off, the card is gone; a waiting
 * invitation puts a dot on the Deen tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class DaroodDeenTabUiTest {

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

    private fun showApp(mode: ThemeMode = ThemeMode.Pixel, root: PqRootState = PqRootState()) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                val nav = rememberNavController()
                PqAppFrame(nav, root) { padding ->
                    NavHost(nav, startDestination = Screen.Home.route, modifier = Modifier.padding(padding)) {
                        composable(Screen.Home.route) { Text("TODAY'S DASHBOARD") }
                        islamicGraph(nav, hub = { onOpen ->
                            val state by hub.state.collectAsState()
                            com.pixelquest.app.islamic.ui.IslamicHubContent(state, onOpen)
                        })
                        // Deen's Darood tile opens the parties, outside the Islamic section.
                        composable(Screen.Parties.route) {
                            com.pixelquest.app.party.ui.PartiesScreen(
                                onNavigateBack = { nav.popBackStack() },
                                onSignIn = { nav.navigate(Screen.Account.route) },
                                onOpenParty = {},
                                onCreateParty = {},
                                viewModel = com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel(auth, repository)
                            )
                        }
                        composable(Screen.Account.route) { Text("ACCOUNT SCREEN") }
                    }
                }
            }
        }
    }

    /** The list comes from the database, which answers on its own thread. */
    private fun waitForTag(tag: String) = composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openTheHub() {
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Deen)).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(com.pixelquest.app.islamic.ui.IslamicTestTags.HUB).assertIsDisplayed()
    }

    private fun openDaroodFromTheDeenTab() {
        openTheHub()
        waitForTag(com.pixelquest.app.islamic.ui.IslamicTestTags.card("darood"))
        composeTestRule.onNodeWithTag(com.pixelquest.app.islamic.ui.IslamicTestTags.card("darood")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(DaroodTestTags.PARTY_SCREEN).assertIsDisplayed()
    }

    @Test
    fun signedOut_theCardOpensTheSignInPrompt_whichOpensTheAccountScreen() {
        showApp(ThemeMode.Pixel)
        openDaroodFromTheDeenTab()

        waitForTag(DaroodTestTags.SIGNED_OUT)
        composeTestRule.onNodeWithTag(DaroodTestTags.SIGNED_OUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.SIGN_IN).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("ACCOUNT SCREEN").assertIsDisplayed()
    }

    @Test
    fun signedIn_theCardOpensThePlaceholder_inComic() {
        auth.userFlow.value = AuthUser(id = "aria-id", email = null, displayName = null)
        showApp(ThemeMode.Comic)
        openDaroodFromTheDeenTab()

        waitForTag(DaroodTestTags.EMPTY)
        composeTestRule.onNodeWithTag(DaroodTestTags.EMPTY).assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Sign in to join a party").fetchSemanticsNodes().size)
    }

    @Test
    fun inLight_theCardOpensToo() {
        showApp(ThemeMode.Light)
        openDaroodFromTheDeenTab()
    }

    @Test
    fun withItsFlagOff_theCardIsGone() {
        settings.daroodAvailable.value = false
        showApp()
        openTheHub()
        composeTestRule.waitForIdle()

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(com.pixelquest.app.islamic.ui.IslamicTestTags.card("darood"))).fetchSemanticsNodes().size)
    }

    @Test
    fun aWaitingInvitation_putsADotOnTheDeenTab() {
        showApp(root = PqRootState(daroodInviteWaiting = true))

        // The tab is one button to TalkBack, so the dot is found in the unmerged tree, and its words
        // in the tab's own description.
        composeTestRule.onNodeWithTag(PqNavTags.dot(PqTab.Deen), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("something waits for you here", substring = true).assertIsDisplayed()
    }

    @Test
    fun noInvitation_noDot() {
        showApp()

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(PqNavTags.dot(PqTab.Deen)), useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
