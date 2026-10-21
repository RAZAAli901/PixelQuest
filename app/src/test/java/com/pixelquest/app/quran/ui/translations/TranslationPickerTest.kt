package com.pixelquest.app.quran.ui.translations

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.RoomTransactionRunner
import com.pixelquest.app.quran.data.AndroidQuranAssetSource
import com.pixelquest.app.quran.data.QuranAssetSource
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranRepositoryImpl
import com.pixelquest.app.quran.data.TranslationChoiceStore
import com.pixelquest.app.quran.data.TranslationSwitcher
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The translation picker on the real Quran: the bundled translations, the terms of the one on the
 * phone, and choosing another, which is kept and loaded. A second translation's file stands in for
 * one added later: only its start, where the app reads who it is.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class TranslationPickerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val real = AndroidQuranAssetSource(context)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val viewModels = TestViewModels()
    private var loads = 0

    private val withASecond = object : QuranAssetSource {
        override fun read(name: String) =
            if (name == "quran_xx.json") """{"version":"v","id":"xx.test","name":"Test Translation","translator":"A Translator","language":"Test"}""" else real.read(name)
        override fun list() = real.list() + "quran_xx.json"
    }

    private val choice = object : TranslationChoiceStore {
        var id: String? = null
        override fun chosen() = id
        override fun choose(id: String) { this.id = id }
    }

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        QuranImporter(real, db.quranDao(), RoomTransactionRunner(db)) { QuranImporter.TRANSLATION_FILE }.import()
        Unit
    }

    @After
    fun tearDown() {
        viewModels.clear()
        db.closeAfterWrites()
        Dispatchers.resetMain()
    }

    private fun show(mode: ThemeMode = ThemeMode.Pixel): TranslationPickerViewModel {
        val vm = viewModels.keep(TranslationPickerViewModel::class.java) {
            TranslationPickerViewModel(TranslationSwitcher(withASecond, choice), QuranRepositoryImpl(db.quranDao())) { loads++ }
        }
        composeTestRule.setContent { PixelQuestTheme(themeMode = mode) { TranslationPickerRoute(onNavigateBack = {}, viewModel = vm) } }
        // Waiting lets the main looper run: blocking it (runBlocking on a flow) can hang the test.
        composeTestRule.waitUntil(5_000) { vm.state.value.let { it.loaded && it.onPhone != null } }
        composeTestRule.waitForIdle()
        return vm
    }

    @Test
    fun theBundledTranslations_withTheOneReadChosen_andItsTerms() {
        show()

        composeTestRule.onNodeWithTag(TranslationTags.option("en.sahih")).assertIsSelected()
        composeTestRule.onNodeWithTag(TranslationTags.option("xx.test")).assertIsNotSelected()
        composeTestRule.onNodeWithText("A Translator · Test", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Terms of Saheeh International").assertIsDisplayed()
        composeTestRule.onNodeWithText("non-commercial", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Last updated by its source: April 24, 2011").assertIsDisplayed()
    }

    @Test
    fun choosingAnother_keepsIt_andLoadsIt() {
        val vm = show(ThemeMode.Comic)

        composeTestRule.onNodeWithTag(TranslationTags.option("xx.test")).performClick()
        // The choice is written off the main thread: wait for it.
        composeTestRule.waitUntil(5_000) { vm.state.value.selected == "xx.test" }
        val chosen = vm.state.value
        composeTestRule.waitForIdle()

        assertEquals("xx.test", choice.id)
        assertEquals(1, loads)
        assertTrue(chosen.switching)
        composeTestRule.onNodeWithTag(TranslationTags.SWITCHING).assertIsDisplayed()
    }

    @Test
    fun choosingTheOneRead_doesNothing() {
        show(ThemeMode.Comic)

        composeTestRule.onNodeWithTag(TranslationTags.option("en.sahih")).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, loads)
        assertFalse(choice.id == "xx.test")
        composeTestRule.onNodeWithTag(TranslationTags.SWITCHING).assertDoesNotExist()
    }
}
