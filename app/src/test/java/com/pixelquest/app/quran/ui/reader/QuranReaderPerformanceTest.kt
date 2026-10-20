package com.pixelquest.app.quran.ui.reader

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.MemoryBookmarks
import com.pixelquest.app.quran.data.MemoryLastReadStore
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
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
import kotlin.system.measureTimeMillis

/**
 * Al-Baqarah, the longest surah (286 ayahs), read smoothly: its pages come from the database in
 * moments, a page arriving never moves what the player is reading, and the reader scrolls through
 * to its end. A phone draws frames, which Robolectric doesn't; these are what keeps them smooth.
 * The time limits are generous, for slow CI machines: on a laptop each takes a small part of them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class QuranReaderPerformanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val quran = QuranRepositoryImpl(db.quranDao())
    private val viewModels = TestViewModels()

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        Unit
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    @Test
    fun alBaqarahsPages_comeFromTheDatabase_inMoments() = runBlocking {
        quran.ayahs(1, 0) // The first query opens the database: not counted.
        val pages = mutableListOf<Int>()
        val took = measureTimeMillis {
            var page = 0
            do {
                val ayahs = quran.ayahs(2, page++)
                pages += ayahs.size
            } while (ayahs.size == QuranRepository.PAGE_SIZE)
        }

        assertEquals(listOf(50, 50, 50, 50, 50, 36), pages)
        assertTrue("all six pages in $took ms", took < 1_000)
    }

    @Test
    fun searches_answerInMoments() = runBlocking {
        quran.searchTranslation("mercy")
        val english = measureTimeMillis { quran.searchTranslation("merciful") }
        val arabic = measureTimeMillis { quran.searchArabic("الرحمن الرحيم") }

        assertTrue("the translation in $english ms", english < 1_000)
        assertTrue("the Arabic in $arabic ms", arabic < 1_000)
    }

    @Test
    fun aPageArriving_neverMovesWhatThePlayerIsReading_andTheReaderScrollsToTheEnd() {
        val vm = viewModels.keep(SurahReaderViewModel::class.java) {
            SurahReaderViewModel(SavedStateHandle(mapOf("surah" to 2)), quran, MemoryReadingSettingsStore(), MemoryBookmarks(), MemoryLastReadStore())
        }
        lateinit var list: LazyListState
        composeTestRule.setContent {
            list = rememberLazyListState()
            val state by vm.state.collectAsState()
            PixelQuestTheme {
                SurahReaderScreen(state = state, onNavigateBack = {}, onLoadMore = vm::loadMore, onScrolled = vm::onScrolled, listState = list)
            }
        }
        composeTestRule.waitUntil(10_000) { vm.state.value.ayahs.size == 50 }

        // Near the first page's end: the next page is asked for, and arrives below, not in the way.
        composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(45)
        composeTestRule.waitForIdle()
        val reading = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        composeTestRule.waitUntil(10_000) { vm.state.value.ayahs.size >= 100 }
        composeTestRule.waitForIdle()
        assertEquals("still where they were", reading, list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset)

        // On through every page, to the surah's last ayah and the way on to the next.
        var index = 45
        while (vm.state.value.hasMore || index < vm.state.value.ayahs.size) {
            index = minOf(index + 40, vm.state.value.ayahs.size)
            composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(index)
            composeTestRule.waitForIdle()
        }
        assertEquals(286, vm.state.value.ayahs.size)
        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasTestTag(ReaderTags.ayah(286)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
