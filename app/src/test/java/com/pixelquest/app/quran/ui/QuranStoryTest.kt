package com.pixelquest.app.quran.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.LastRead
import com.pixelquest.app.quran.data.MemoryLastReadStore
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.data.RoomBookmarkRepository
import com.pixelquest.app.quran.ui.bookmarks.BookmarkTags
import com.pixelquest.app.quran.ui.bookmarks.SavedBookmarks
import com.pixelquest.app.quran.ui.bookmarks.BookmarksViewModel
import com.pixelquest.app.quran.ui.list.CONTINUE_READING
import com.pixelquest.app.quran.ui.list.QuranListTags
import com.pixelquest.app.quran.ui.list.QuranTab
import com.pixelquest.app.quran.ui.list.SurahListViewModel
import com.pixelquest.app.quran.ui.reader.AyahAction
import com.pixelquest.app.quran.ui.reader.AyahFlash
import com.pixelquest.app.quran.ui.reader.DisplayMode
import com.pixelquest.app.quran.ui.reader.ReaderTags
import com.pixelquest.app.quran.ui.reader.SurahReaderRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderViewModel
import com.pixelquest.app.quran.ui.search.QuranSearchRoute
import com.pixelquest.app.quran.ui.search.QuranSearchViewModel
import com.pixelquest.app.quran.ui.search.SearchTags
import com.pixelquest.app.ui.navigation.Screen
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

/**
 * The Quran reader as a player uses it, on the real Quran: the real screens and view models, linked
 * as quranScreens links them (its own graph here, as Hilt makes the app's view models). Opening
 * Al-Baqarah and scrolling it, switching what each ayah shows, bookmarking an ayah and finding it
 * again, searching and landing on the ayah found, and carrying on from the last place read.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class QuranStoryTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val importer = QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }
    private val quran = QuranRepositoryImpl(db.quranDao())
    private val bookmarks = RoomBookmarkRepository(db.quranBookmarkDao()) { 1L }
    private val settings = MemoryReadingSettingsStore()
    private val lastRead = MemoryLastReadStore()
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

    /** The Quran's screens, linked as quranScreens links them. */
    @Composable
    private fun Quran() {
        val nav = rememberNavController()
        NavHost(nav, startDestination = Screen.Quran.route) {
            composable(Screen.Quran.route) {
                QuranRoute(
                    onNavigateBack = {},
                    onOpenSurah = { nav.navigate(Screen.QuranSurah.createRoute(it)) },
                    onOpenAyah = { s, a -> nav.navigate(Screen.QuranSurah.createRoute(s, a)) },
                    onOpenSearch = { nav.navigate(Screen.QuranSearch.route) },
                    viewModel = viewModel { QuranViewModel(importer, db.quranDao()) },
                    listViewModel = viewModel { SurahListViewModel(quran, lastRead) },
                    saved = {
                        SavedBookmarks(
                            onOpenAyah = { s, a -> nav.navigate(Screen.QuranSurah.createRoute(s, a)) },
                            viewModel = viewModel { BookmarksViewModel(bookmarks, quran) }
                        )
                    }
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
                    viewModel = viewModel { SurahReaderViewModel(createSavedStateHandle(), quran, settings, bookmarks, lastRead) }
                )
            }
            composable(Screen.QuranSearch.route) {
                QuranSearchRoute(
                    onNavigateBack = { nav.popBackStack() },
                    onOpenSurah = { nav.navigate(Screen.QuranSurah.createRoute(it)) },
                    onOpenAyah = { s, a -> nav.navigate(Screen.QuranSurah.createRoute(s, a)) },
                    viewModel = viewModel { QuranSearchViewModel(quran) }
                )
            }
        }
    }

    private fun open() {
        composeTestRule.setContent { PixelQuestTheme { Quran() } }
        waitForTag(QuranListTags.row(2))
    }

    private fun waitForTag(tag: String) = composeTestRule.waitUntil(10_000) {
        composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForNoTag(tag: String) = composeTestRule.waitUntil(10_000) {
        composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }

    private fun back() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun alBaqarah_opens_scrollsOnPastItsFirstPage_andShowsEachMode() {
        open()

        composeTestRule.onNodeWithTag(QuranListTags.row(2)).performClick()
        waitForTag(ReaderTags.ayah(1))
        composeTestRule.onNodeWithText("Al-Baqara", substring = true).assertIsDisplayed()

        // The first page is 50 ayahs (and the Bismillah): scrolling to its end loads the next.
        // The list composes only what's on screen: it can be scrolled to index 100 once that's loaded.
        composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(50)
        composeTestRule.waitUntil(10_000) { runCatching { composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(100) }.isSuccess }
        waitForTag(ReaderTags.ayah(100))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(100)).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.ARABIC)).performClick()
        waitForNoTag(ReaderTags.translation(100))
        composeTestRule.onNodeWithTag(ReaderTags.arabic(100), useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.TRANSLATION)).performClick()
        waitForNoTag(ReaderTags.arabic(100))
        composeTestRule.onNodeWithTag(ReaderTags.translation(100), useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.BOTH)).performClick()
        waitForTag(ReaderTags.arabic(100))
        composeTestRule.onNodeWithTag(ReaderTags.translation(100), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun anAyahBookmarked_isMarked_andFoundAgainInBookmarks() {
        open()
        composeTestRule.onNodeWithTag(QuranListTags.row(2)).performClick()
        waitForTag(ReaderTags.ayah(5))

        composeTestRule.onNodeWithTag(ReaderTags.ayah(5)).performClick()
        waitForTag(ReaderTags.action(AyahAction.BOOKMARK))
        composeTestRule.onNodeWithTag(ReaderTags.action(AyahAction.BOOKMARK)).performClick()
        waitForTag(ReaderTags.RIBBON)

        back()
        waitForTag(QuranListTags.row(2))
        // The Saved tab holds the bookmarks.
        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.SAVED)).performClick()
        waitForTag(BookmarkTags.row(2, 5))

        composeTestRule.onNodeWithText("Ayah 2:5").performClick()
        waitForTag(ReaderTags.ayah(5))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(5)).assertIsDisplayed()
    }

    @Test
    fun aSearch_opensTheReaderAtTheAyahFound_andMarksIt() {
        open()
        composeTestRule.onNodeWithTag(SearchTags.ACTION).performClick()
        waitForTag(SearchTags.FIELD)

        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(SearchTags.FIELD))).performTextInput("kursi")
        // Saheeh International's word in Ayat al-Kursi.
        waitForTag(SearchTags.ayah(2, 255))

        composeTestRule.onNodeWithTag(SearchTags.ayah(2, 255)).performClick()
        waitForTag(ReaderTags.ayah(255))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(255)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.ayah(255)).assert(SemanticsMatcher.expectValue(AyahFlash, true))
    }

    @Test
    fun continueReading_takesThePlayerBackWhereTheyStopped() {
        open()
        composeTestRule.onNodeWithTag(QuranListTags.row(2)).performClick()
        waitForTag(ReaderTags.ayah(1))

        composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(40)
        composeTestRule.waitForIdle()
        // The place is kept once the player settles there.
        main.scheduler.advanceTimeBy(SurahReaderViewModel.KEEP_PLACE_AFTER_MS + 100)
        val place = lastRead.lastRead.value
        assertTrue("a place in Al-Baqarah, past its start: $place", place != null && place.surah == 2 && place.ayah > 30)

        back()
        waitForTag(CONTINUE_READING)
        composeTestRule.onNodeWithText("Ayah ${place!!.ayah} / 286", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag(CONTINUE_READING).performClick()
        waitForTag(ReaderTags.ayah(place.ayah))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(place.ayah)).assertIsDisplayed()
        assertEquals(LastRead(2, place.ayah), lastRead.lastRead.value)
    }
}
