package com.pixelquest.app.quran.ui.bookmarks

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.data.RoomBookmarkRepository
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The bookmarks screen on the real Quran: grouped by surah, opening the reader, removable, and empty at first. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class BookmarksTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val bookmarks = RoomBookmarkRepository(db.quranBookmarkDao()) { 1L }
    private val quran = QuranRepositoryImpl(db.quranDao())
    private val viewModels = TestViewModels()
    private val opened = mutableListOf<Pair<Int, Int>>()

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

    private fun show(): BookmarksViewModel {
        val vm = viewModels.keep(BookmarksViewModel::class.java) { BookmarksViewModel(bookmarks, quran) }
        composeTestRule.setContent {
            PixelQuestTheme { BookmarksRoute(onNavigateBack = {}, onOpenAyah = { s, a -> opened += s to a }, viewModel = vm) }
        }
        return vm
    }

    @Test
    fun withNone_itSaysHowToAddOne() {
        show()
        composeTestRule.waitUntil(5_000) { composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("No bookmarks yet")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun bookmarks_groupedBySurah_openTheReader() = runBlocking {
        bookmarks.toggle(36, 1)
        bookmarks.toggle(2, 255)
        bookmarks.toggle(2, 1)
        val vm = show()
        vm.state.first { it.loaded && it.groups.size == 2 }

        assertEquals(listOf(2, 36), vm.state.value.groups.map { it.surah })
        assertEquals(listOf(1, 255), vm.state.value.groups.first().ayahs.map { it.number })
        assertEquals("the ayah as the database holds it", quran.ayah(2, 255)!!.translation, vm.state.value.groups.first().ayahs[1].translation)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2. Al-Baqara").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ayah 2:255").performClick()
        assertEquals(listOf(2 to 255), opened)
    }

    @Test
    fun theCross_removesABookmark() = runBlocking {
        bookmarks.toggle(2, 255)
        val vm = show()
        vm.state.first { it.groups.isNotEmpty() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Remove the bookmark on 2:255").performClick()

        vm.state.first { it.groups.isEmpty() }
        assertTrue(bookmarks.all().first().isEmpty())
    }
}
