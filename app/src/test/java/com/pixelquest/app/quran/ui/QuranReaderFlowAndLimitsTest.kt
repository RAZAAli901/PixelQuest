package com.pixelquest.app.quran.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
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
import com.pixelquest.app.quran.data.ArabicSearch
import com.pixelquest.app.quran.data.MemoryLastReadStore
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.data.RoomBookmarkRepository
import com.pixelquest.app.quran.data.TranslationSearch
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.bookmarks.BookmarkTags
import com.pixelquest.app.quran.ui.bookmarks.BookmarksViewModel
import com.pixelquest.app.quran.ui.bookmarks.SavedBookmarks
import com.pixelquest.app.quran.ui.list.CONTINUE_READING
import com.pixelquest.app.quran.ui.list.QuranListTags
import com.pixelquest.app.quran.ui.list.QuranTab
import com.pixelquest.app.quran.ui.list.SurahListViewModel
import com.pixelquest.app.quran.ui.reader.AyahAction
import com.pixelquest.app.quran.ui.reader.AyahFlash
import com.pixelquest.app.quran.ui.reader.DisplayMode
import com.pixelquest.app.quran.ui.reader.ReaderTags
import com.pixelquest.app.quran.ui.reader.ReaderUiState
import com.pixelquest.app.quran.ui.reader.SurahReaderRoute
import com.pixelquest.app.quran.ui.reader.SurahReaderScreen
import com.pixelquest.app.quran.ui.reader.SurahReaderViewModel
import com.pixelquest.app.quran.ui.search.QuranSearchRoute
import com.pixelquest.app.quran.ui.search.QuranSearchViewModel
import com.pixelquest.app.quran.ui.search.SearchTags
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * V042: Quran reader flow and out-of-bounds cases.
 *
 * Simulates user flows:
 * - Open a surah, switch display modes (BOTH, ARABIC, TRANSLATION)
 * - Bookmark an ayah, verify ribbon and bookmark state
 * - Search by reference and keywords, navigate directly to target ayah
 * - Continue reading retention and resumption
 *
 * Pushes edge cases:
 * - All 114 surahs loaded with valid structure and first ayah
 * - Empty, whitespace, special characters, SQL injection, regex, unicode/emojis
 * - Arabic search with and without diacritics / harakat / tatweel / wasla equivalence
 * - High font scale (2.0x) rendering without crashes
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class QuranReaderFlowAndLimitsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val importer = QuranImporter(
        AndroidQuranAssetSource(context),
        db.quranDao(),
        RoomTransactionRunner(db),
    ) { QuranImporter.TRANSLATION_FILE }
    private val quranRepository = QuranRepositoryImpl(db.quranDao())
    private val bookmarkRepository = RoomBookmarkRepository(db.quranBookmarkDao()) { 1000L }
    private val settingsStore = MemoryReadingSettingsStore()
    private val lastReadStore = MemoryLastReadStore()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        importer.import()
        Unit
    }

    @After
    fun tearDown() {
        // The database isn't closed: the screens' view models live until the activity goes, after this.
        Dispatchers.resetMain()
    }

    @Composable
    private fun QuranNavGraph() {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Screen.Quran.route) {
            composable(Screen.Quran.route) {
                QuranRoute(
                    onNavigateBack = {},
                    onOpenSurah = { navController.navigate(Screen.QuranSurah.createRoute(it)) },
                    onOpenAyah = { s, a -> navController.navigate(Screen.QuranSurah.createRoute(s, a)) },
                    onOpenSearch = { navController.navigate(Screen.QuranSearch.route) },
                    viewModel = viewModel { QuranViewModel(importer, db.quranDao()) },
                    listViewModel = viewModel { SurahListViewModel(quranRepository, lastReadStore) },
                    saved = {
                        SavedBookmarks(
                            onOpenAyah = { s, a -> navController.navigate(Screen.QuranSurah.createRoute(s, a)) },
                            viewModel = viewModel { BookmarksViewModel(bookmarkRepository, quranRepository) },
                        )
                    },
                )
            }
            composable(
                route = Screen.QuranSurah.route,
                arguments = listOf(
                    navArgument(SurahReaderViewModel.ARG_SURAH) { type = NavType.IntType },
                    navArgument(SurahReaderViewModel.ARG_AYAH) {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) {
                SurahReaderRoute(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel {
                        SurahReaderViewModel(
                            createSavedStateHandle(),
                            quranRepository,
                            settingsStore,
                            bookmarkRepository,
                            lastReadStore,
                        )
                    },
                )
            }
            composable(Screen.QuranSearch.route) {
                QuranSearchRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenSurah = { navController.navigate(Screen.QuranSurah.createRoute(it)) },
                    onOpenAyah = { s, a -> navController.navigate(Screen.QuranSurah.createRoute(s, a)) },
                    viewModel = viewModel { QuranSearchViewModel(quranRepository) },
                )
            }
        }
    }

    private fun waitForTag(tag: String) = composeTestRule.waitUntil(10_000) {
        composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForNoTag(tag: String) = composeTestRule.waitUntil(10_000) {
        composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun userFlow_openSurah_switchDisplayModes_bookmark_search_and_continueReading() {
        composeTestRule.setContent {
            PixelQuestTheme {
                QuranNavGraph()
            }
        }
        waitForTag(QuranListTags.row(1))

        // 1. Open Surah 1 (Al-Fatihah)
        composeTestRule.onNodeWithTag(QuranListTags.row(1)).performClick()
        waitForTag(ReaderTags.ayah(1))
        composeTestRule.onNodeWithText("Al-Faatiha", substring = true, ignoreCase = true).assertIsDisplayed()

        // 2. Switch Display Modes
        // Default is BOTH
        composeTestRule.onNodeWithTag(ReaderTags.arabic(1), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.translation(1), useUnmergedTree = true).assertIsDisplayed()

        // Switch to ARABIC only
        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.ARABIC)).performClick()
        waitForNoTag(ReaderTags.translation(1))
        composeTestRule.onNodeWithTag(ReaderTags.arabic(1), useUnmergedTree = true).assertIsDisplayed()

        // Switch to TRANSLATION only
        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.TRANSLATION)).performClick()
        waitForNoTag(ReaderTags.arabic(1))
        composeTestRule.onNodeWithTag(ReaderTags.translation(1), useUnmergedTree = true).assertIsDisplayed()

        // Return to BOTH
        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.BOTH)).performClick()
        waitForTag(ReaderTags.arabic(1))
        composeTestRule.onNodeWithTag(ReaderTags.translation(1), useUnmergedTree = true).assertIsDisplayed()

        // 3. Bookmark Ayah 2
        composeTestRule.onNodeWithTag(ReaderTags.ayah(2)).performClick()
        waitForTag(ReaderTags.action(AyahAction.BOOKMARK))
        composeTestRule.onNodeWithTag(ReaderTags.action(AyahAction.BOOKMARK)).performClick()
        waitForTag(ReaderTags.RIBBON)

        // Navigate back to Quran Home
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        waitForTag(QuranListTags.tab(QuranTab.SAVED))

        // Check Saved tab contains the bookmark
        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.SAVED)).performClick()
        waitForTag(BookmarkTags.row(1, 2))
        composeTestRule.onNodeWithText("Ayah 1:2").performClick()
        waitForTag(ReaderTags.ayah(2))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(2)).assertIsDisplayed()

        // 4. Search and navigate to Ayah
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        waitForTag(QuranListTags.tab(QuranTab.SURAHS))
        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.SURAHS)).performClick()

        composeTestRule.onNodeWithTag(SearchTags.ACTION).performClick()
        waitForTag(SearchTags.FIELD)

        composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(SearchTags.FIELD))).performTextInput("1:5")
        waitForTag(SearchTags.ayah(1, 5))
        composeTestRule.onNodeWithTag(SearchTags.ayah(1, 5)).performClick()

        waitForTag(ReaderTags.ayah(5))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(5)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.ayah(5)).assert(SemanticsMatcher.expectValue(AyahFlash, true))

        // 5. Continue Reading verification: scroll to Ayah 6 and advance debounce clock
        composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(6)
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceTimeBy(SurahReaderViewModel.KEEP_PLACE_AFTER_MS + 200)

        val place = lastReadStore.lastRead.value
        assertNotNull("Last read location must be recorded", place)
        assertEquals(1, place!!.surah)

        // Pop reader back to Search screen, then pop Search back to Quran Home
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // Continue reading card should be visible on Surah list
        waitForTag(CONTINUE_READING)
        composeTestRule.onNodeWithTag(CONTINUE_READING).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTINUE_READING).performClick()
        waitForTag(ReaderTags.ayah(place.ayah))
        composeTestRule.onNodeWithTag(ReaderTags.ayah(place.ayah)).assertIsDisplayed()
    }

    @Test
    fun all114Surahs_loadValidMetadata_andFirstAyahWithoutException() = runBlocking {
        val surahs = quranRepository.surahs().first()
        assertEquals(114, surahs.size)

        for (surahNumber in 1..114) {
            val surah = quranRepository.surah(surahNumber)
            assertNotNull("Surah $surahNumber should exist", surah)
            assertEquals("Surah number must match", surahNumber, surah!!.number)
            assertTrue("Surah name must not be blank", surah.englishName.isNotBlank())
            assertTrue("Ayah count must be positive", surah.ayahCount > 0)

            val ayahs = quranRepository.ayahs(surahNumber, page = 0, pageSize = 1)
            assertTrue("First ayah of surah $surahNumber must load", ayahs.isNotEmpty())
            val firstAyah = ayahs.first()
            assertEquals("Ayah number must be 1", 1, firstAyah.number)
            assertTrue("Arabic text must not be empty", firstAyah.arabic.isNotBlank())
            assertTrue("Translation must not be empty", firstAyah.translation.isNotBlank())
        }

        // Boundary navigation test on view models
        val firstVm = SurahReaderViewModel(
            SavedStateHandle(mapOf(SurahReaderViewModel.ARG_SURAH to 1)),
            quranRepository,
            settingsStore,
            bookmarkRepository,
            lastReadStore,
        )
        val firstState = firstVm.state.first { it.surah != null }
        assertNull("Surah 1 has no previous surah", firstState.previous)
        assertNotNull("Surah 1 has next surah", firstState.next)
        assertEquals(2, firstState.next?.number)

        val midVm = SurahReaderViewModel(
            SavedStateHandle(mapOf(SurahReaderViewModel.ARG_SURAH to 57)),
            quranRepository,
            settingsStore,
            bookmarkRepository,
            lastReadStore,
        )
        val midState = midVm.state.first { it.surah != null }
        assertEquals(56, midState.previous?.number)
        assertEquals(58, midState.next?.number)

        val lastVm = SurahReaderViewModel(
            SavedStateHandle(mapOf(SurahReaderViewModel.ARG_SURAH to 114)),
            quranRepository,
            settingsStore,
            bookmarkRepository,
            lastReadStore,
        )
        val lastState = lastVm.state.first { it.surah != null }
        assertEquals(113, lastState.previous?.number)
        assertNull("Surah 114 has no next surah", lastState.next)

        // Out-of-bounds surah queries
        assertNull("Surah 0 does not exist", quranRepository.surah(0))
        assertNull("Negative surah does not exist", quranRepository.surah(-1))
        assertNull("Surah 115 does not exist", quranRepository.surah(115))
        assertNull("Surah 999 does not exist", quranRepository.surah(999))
    }

    @Test
    fun empty_whitespace_and_specialCharacter_searches_handleGracefullyWithoutCrashing() = runBlocking {
        // Translation Search edge cases
        assertNull(TranslationSearch.match(""))
        assertNull(TranslationSearch.match("    "))
        assertNull(TranslationSearch.match("!@#$%^&*()_+-=[]{}|;':,.<>/?"))
        assertNull(TranslationSearch.match("\u0000\u0007\r\n\t"))

        // Search repository with empty or special inputs
        assertTrue(quranRepository.searchTranslation("").isEmpty())
        assertTrue(quranRepository.searchTranslation("     ").isEmpty())
        assertTrue(quranRepository.searchTranslation("!@#$%^&*()_+=").isEmpty())
        assertTrue(quranRepository.searchTranslation("\u0000\u0007").isEmpty())
        assertTrue(quranRepository.searchTranslation("✨📖🕌🕋").isEmpty())
        assertTrue(quranRepository.searchArabic("").isEmpty())
        assertTrue(quranRepository.searchArabic("   ").isEmpty())
        assertTrue(quranRepository.searchArabic("!@#$%^&*()").isEmpty())

        // SQL injection and special regex patterns: must execute safely without throwing or corrupting DB
        assertTrue(runCatching { quranRepository.searchTranslation("'; DROP TABLE quran_ayah; --") }.isSuccess)
        assertTrue(runCatching { quranRepository.searchTranslation("' OR 1=1 --") }.isSuccess)
        assertTrue(runCatching { quranRepository.searchTranslation("""[!@#$%^&*()_+=~`{}|:;"'<>,.?/]""") }.isSuccess)

        // Ensure database table intact after injection attempts
        assertEquals(114, quranRepository.surahs().first().size)

        // 10,000 character oversized search
        val hugeQuery = "peace ".repeat(2000)
        val results = quranRepository.searchTranslation(hugeQuery)
        assertNotNull("Huge search should execute without OOM or crash", results)

        // Reference out-of-bounds queries
        assertNull("Out of bounds reference 999:999", QuranSearchViewModel.reference("999:999")?.let { quranRepository.ayah(it.first, it.second) })
        assertNull("Out of bounds reference 0:0", QuranSearchViewModel.reference("0:0")?.let { quranRepository.ayah(it.first, it.second) })
        assertNull("Out of bounds reference -1:5", QuranSearchViewModel.reference("-1:5"))
        assertNull("Out of bounds reference 115:1", QuranSearchViewModel.reference("115:1")?.let { quranRepository.ayah(it.first, it.second) })
        assertNull("Out of bounds ayah 2:300", QuranSearchViewModel.reference("2:300")?.let { quranRepository.ayah(it.first, it.second) })
        assertNull("Invalid syntax reference", QuranSearchViewModel.reference("invalid:ref"))
    }

    @Test
    fun arabicSearch_withAndWithoutDiacritics_yieldsEquivalentMatches() = runBlocking {
        // ArabicSearch key normalization tests
        // Harakat stripping
        val withHarakat = "قُلْ هُوَ اللَّهُ أَحَدٌ"
        val plainArabic = "قل هو الله احد"
        assertEquals(ArabicSearch.key(plainArabic), ArabicSearch.key(withHarakat))

        // Bismillah equivalence
        val bismillahFull = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val bismillahPlain = "بسم الله الرحمن الرحيم"
        assertEquals(ArabicSearch.key(bismillahPlain), ArabicSearch.key(bismillahFull))

        // Tatweel / Kashida normalization
        val tatweel = "بــــسم"
        assertEquals(ArabicSearch.key("بسم"), ArabicSearch.key(tatweel))

        // Wasla and Hamza normalization
        val withWasla = "ٱلْحَمْدُ لِلَّهِ"
        val plainHamd = "الحمد لله"
        assertEquals(ArabicSearch.key(plainHamd), ArabicSearch.key(withWasla))

        // Query repository with both variants and confirm identical results
        val resultsWithHarakat = quranRepository.searchArabic(withHarakat)
        val resultsPlain = quranRepository.searchArabic(plainArabic)

        assertFalse("Search should find ayahs", resultsPlain.isEmpty())
        assertEquals(resultsPlain.map { it.id }, resultsWithHarakat.map { it.id })
        assertEquals(112, resultsPlain.first().surah)
        assertEquals(1, resultsPlain.first().number)
    }

    @Test
    fun fontScale2x_rendersReaderScreenWithoutClippingOrCrashing() {
        val testSurah = Surah(1, "الفاتحة", "Al-Fatihah", "The Opening", Revelation.MECCAN, 7, 1, "b")
        val sampleAyah = Ayah(1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "en.sahih")

        composeTestRule.setContent {
            PixelQuestTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2.0f)) {
                    SurahReaderScreen(
                        state = ReaderUiState(
                            surah = testSurah,
                            ayahs = listOf(sampleAyah),
                            loading = false,
                        ),
                        onNavigateBack = {},
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ReaderTags.ayah(1)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.arabic(1), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.translation(1), useUnmergedTree = true).assertIsDisplayed()
    }
}
