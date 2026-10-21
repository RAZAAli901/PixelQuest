package com.pixelquest.app.integration

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.R
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.auth.FakeAuthRepository
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.darood.ui.DaroodPartyScreen
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.chat.PartyChatRoute
import com.pixelquest.app.darood.ui.chat.PartyChatViewModel
import com.pixelquest.app.darood.ui.list.DaroodPartiesViewModel
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.islamic.ui.IslamicHubContent
import com.pixelquest.app.islamic.ui.IslamicHubUiState
import com.pixelquest.app.islamic.ui.IslamicTestTags
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.MemoryLastReadStore
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.data.RoomBookmarkRepository
import com.pixelquest.app.quran.ui.list.QuranListTags
import com.pixelquest.app.quran.ui.QuranRoute
import com.pixelquest.app.quran.ui.QuranViewModel
import com.pixelquest.app.quran.ui.list.SurahListViewModel
import com.pixelquest.app.quran.ui.reader.ReaderTags
import com.pixelquest.app.quran.ui.reader.SurahReaderRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderViewModel
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeNamazRepository
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
import com.pixelquest.app.ui.screens.namaz.NamazScreen
import com.pixelquest.app.ui.screens.namaz.NamazTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * The Islamic side of the app in one run, through its real screens and view models (in memory, as
 * Hilt would make them): signing in, opening the hub from the side menu, marking Asr in the Namaz
 * Tracker, posting Darood in a party, and reading a surah of the real Quran.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class IslamicEndToEndTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    // The Quran, loaded from the app's own files.
    private val importer = QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }
    private val quran = QuranRepositoryImpl(db.quranDao())

    // Namaz on 17 October 2026 at 16:00 in Lahore, the tracker on.
    private val namaz = FakeNamazRepository(com.pixelquest.app.data.local.entity.NamazSettingsEntity(enabled = true))

    // Darood Party: signed out to start; a party waiting for Aria once she's in.
    private val auth = FakeAuthRepository()
    private val server = FakeDaroodServer(me = "aria").apply { usernames += mapOf("aria" to "aria_k", "bram" to "bram") }
    private val party = server.seedParty("Friday circle", "aria", "bram")
    private val darood = DaroodPartyRepositoryImpl(server, db.daroodDao(), { auth.userFlow.value?.id }, now = { 1_000L }, zone = { ZoneId.of("UTC") })

    private lateinit var nav: NavHostController
    private val namazVm by lazy { namazViewModel(namaz) }
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(main)
        importer.import()
        Unit
    }

    @After
    fun tearDown() {
        // The database isn't closed: the screens' view models live until the activity goes, after this.
        Dispatchers.resetMain()
    }

    private val cards = listOf(
        IslamicItem("namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), Screen.Namaz.route),
        IslamicItem("darood", R.string.darood_title, R.string.islamic_darood_description, IslamicIcon("📿"), Screen.DaroodParty.route),
        IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), Screen.Quran.route)
    )

    private fun showApp() {
        composeTestRule.setContent {
            PixelQuestTheme {
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
                        composable(Screen.Home.route) {
                            Row {
                                MenuTopBarAction()
                                Text("TODAY'S DASHBOARD")
                            }
                        }
                        // The account screen, as far as this run needs it: signing in.
                        composable(Screen.Account.route) {
                            Column {
                                Button(onClick = {
                                    auth.userFlow.value = AuthUser("aria", "aria@pixelquest.test", "Aria")
                                    nav.popBackStack()
                                }, modifier = Modifier.testTag(SIGN_IN_DONE)) { Text("Sign in") }
                            }
                        }
                        islamicGraph(
                            nav,
                            items = emptyList(),
                            hub = { onOpen -> IslamicHubContent(IslamicHubUiState(cards, loaded = true), onOpen) }
                        ) {
                            composable(Screen.Namaz.route) { NamazScreen(onNavigateBack = { nav.popBackStack() }, viewModel = namazVm) }
                            composable(Screen.DaroodParty.route) {
                                DaroodPartyScreen(
                                    onNavigateBack = { nav.popBackStack() },
                                    onSignIn = { nav.navigate(Screen.Account.route) },
                                    onOpenParty = { nav.navigate(Screen.DaroodPartyChat.createRoute(it)) },
                                    viewModel = viewModel { DaroodPartiesViewModel(auth, darood, null, SavedStateHandle()) }
                                )
                            }
                            composable(Screen.DaroodPartyChat.route, arguments = listOf(navArgument("partyId") { type = NavType.StringType })) { entry ->
                                PartyChatRoute(
                                    partyId = entry.arguments?.getString("partyId").orEmpty(),
                                    onNavigateBack = { nav.popBackStack() },
                                    onOpenInfo = {},
                                    viewModel = viewModel { PartyChatViewModel(createSavedStateHandle(), darood, { auth.userFlow.value?.id }) }
                                )
                            }
                            composable(Screen.Quran.route) {
                                QuranRoute(
                                    onNavigateBack = { nav.popBackStack() },
                                    onOpenSurah = { nav.navigate(Screen.QuranSurah.createRoute(it)) },
                                    onOpenAyah = { s, a -> nav.navigate(Screen.QuranSurah.createRoute(s, a)) },
                                    onOpenBookmarks = {},
                                    onOpenSearch = {},
                                    viewModel = viewModel { QuranViewModel(importer, db.quranDao()) },
                                    listViewModel = viewModel { SurahListViewModel(quran, MemoryLastReadStore()) }
                                )
                            }
                            composable(
                                route = Screen.QuranSurah.route,
                                arguments = listOf(
                                    navArgument(SurahReaderViewModel.ARG_SURAH) { type = NavType.IntType },
                                    navArgument(SurahReaderViewModel.ARG_AYAH) { type = NavType.IntType; defaultValue = 0 }
                                )
                            ) {
                                SurahReaderRoute(
                                    onNavigateBack = { nav.popBackStack() },
                                    viewModel = viewModel {
                                        SurahReaderViewModel(
                                            createSavedStateHandle(), quran, MemoryReadingSettingsStore(),
                                            RoomBookmarkRepository(db.quranBookmarkDao()) { 1L }, MemoryLastReadStore()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun waitForTag(tag: String) = composeTestRule.waitUntil(10_000) {
        composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun current() = composeTestRule.runOnIdle { nav.currentDestination?.route }

    private fun back() {
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }

    @Test
    fun signIn_openTheHub_markAPrayer_postDarood_andReadASurah() {
        showApp()

        // The side menu's Islamic entry opens the hub.
        composeTestRule.onNodeWithContentDescription("Open menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ISLAMIC").performClick()
        waitForTag(IslamicTestTags.HUB)

        // Namaz: Asr marked as prayed.
        composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).performClick()
        composeTestRule.waitUntil(10_000) { namazVm.uiState.value.enabled && namazVm.uiState.value.slots.isNotEmpty() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).performScrollToNode(hasTestTag(NamazTestTags.prayer(Prayer.ASR)))
        composeTestRule.onNodeWithTag(NamazTestTags.prayer(Prayer.ASR)).performClick()
        composeTestRule.waitUntil(10_000) {
            runBlocking { namaz.getDay(LocalDate.of(2026, 10, 17)) }.any { it.prayer == Prayer.ASR && it.status == PrayerStatus.PRAYED }
        }
        back()
        assertEquals(Screen.IslamicHub.route, current())

        // Darood Party: signed out, it asks to sign in; signed in, the party is there.
        composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).performClick()
        waitForTag(DaroodTestTags.SIGN_IN)
        composeTestRule.onNodeWithTag(DaroodTestTags.SIGN_IN).performClick()
        waitForTag(SIGN_IN_DONE)
        composeTestRule.onNodeWithTag(SIGN_IN_DONE).performClick()
        waitForTag(DaroodTestTags.partyRow(party))

        // Posting 313 in it: the bubble is sent, and the party's total is 313.
        composeTestRule.onNodeWithTag(DaroodTestTags.partyRow(party)).performClick()
        waitForTag(DaroodTestTags.CHAT_INPUT)
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT).performTextInput("313")
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_SEND).performClick()
        composeTestRule.waitUntil(10_000) { server.total(party) == 313L }
        waitForTag(DaroodTestTags.ENTRY_SENT)
        back()
        back()
        assertEquals(Screen.IslamicHub.route, current())

        // The Quran: Al-Fatihah opens on its first ayah.
        composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).performClick()
        waitForTag(QuranListTags.row(1))
        composeTestRule.onNodeWithTag(QuranListTags.row(1)).performClick()
        waitForTag(ReaderTags.ayah(1))
        composeTestRule.onNodeWithText("Al-Faatiha", substring = true).assertExists()
        back()
        back()
        assertEquals(Screen.IslamicHub.route, current())

        // And all of it in one place: the prayer, the entry, and the Quran still whole.
        assertTrue(runBlocking { namaz.getDay(LocalDate.of(2026, 10, 17)) }.any { it.prayer == Prayer.ASR })
        assertEquals(1, server.entries.count { it.partyId == party && it.userId == "aria" })
        assertEquals(6_236, runBlocking { db.quranDao().ayahCount() })
    }

    private companion object {
        const val SIGN_IN_DONE = "e2e_sign_in_done"
    }
}
