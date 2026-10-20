package com.pixelquest.app.quran.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranLoadState
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one-time "Preparing the Quran" message: shown until the Quran is on the phone, with how far
 * the load has got; gone once it's loaded; and the load itself well within its 5 seconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class QuranPreparingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val importer = QuranImporter(AndroidQuranAssetSource(context), db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }
    private val viewModels = TestViewModels()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    @Test
    fun beforeTheLoad_itsPreparing_andAfter_itsReady() = runBlocking {
        val vm = viewModels.keep(QuranViewModel::class.java) { QuranViewModel(importer, db.quranDao()) }
        val job = launch(Dispatchers.Unconfined) { vm.preparing.collect {} }

        assertEquals(QuranPreparing(), vm.preparing.value)
        importer.import()
        assertNull(vm.preparing.first { it == null })
        job.cancel()
    }

    @Test
    fun theLoad_goesFrom0To1_andEndsDone() = runBlocking {
        val seen = mutableListOf<Float>()
        val job = launch(Dispatchers.Unconfined) {
            importer.state.collect { if (it is QuranLoadState.Loading) seen += it.progress }
        }
        importer.import()
        job.cancel()

        assertEquals(0f, seen.first())
        assertEquals(1f, seen.last(), 0.0001f)
        assertEquals(seen.sorted(), seen)
        assertTrue(importer.state.value is QuranLoadState.Done)
    }

    @Test
    fun aLoad_takesWellUnderFiveSeconds() = runBlocking {
        importer.import() // the first warms the JVM up
        val started = System.nanoTime()
        importer.import()
        val millis = (System.nanoTime() - started) / 1_000_000
        assertTrue("a load took $millis ms", millis < 5_000)
    }

    @Test
    fun theMessage_saysWhatsHappening_withAProgressBar() {
        composeTestRule.setContent {
            PixelQuestTheme { QuranHomeScreen(onNavigateBack = {}, preparing = QuranPreparing(0.5f)) }
        }

        composeTestRule.onNodeWithTag(QURAN_PREPARING).assertIsDisplayed()
        composeTestRule.onNodeWithText("This happens once", substring = true).assertIsDisplayed()
        val bar = composeTestRule.onAllNodes(hasTestTag(QURAN_PREPARING).not()).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrElseNullable(SemanticsProperties.ProgressBarRangeInfo) { null } }
        assertEquals(listOf(ProgressBarRangeInfo(0.5f, 0f..1f)), bar)
    }

    @Test
    fun onceReady_theMessageIsGone_forTheSurahs() {
        composeTestRule.setContent {
            PixelQuestTheme { QuranHomeScreen(onNavigateBack = {}, preparing = null) }
        }

        assertEquals(0, composeTestRule.onAllNodes(hasTestTag(QURAN_PREPARING)).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag(com.pixelquest.app.quran.ui.list.QuranListTags.SEARCH).assertIsDisplayed()
    }
}
