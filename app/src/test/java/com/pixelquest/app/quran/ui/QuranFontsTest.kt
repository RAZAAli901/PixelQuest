package com.pixelquest.app.quran.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.bookmarks.BookmarkGroup
import com.pixelquest.app.quran.ui.bookmarks.BookmarkTags
import com.pixelquest.app.quran.ui.bookmarks.BookmarksList
import com.pixelquest.app.quran.ui.bookmarks.BookmarksUiState
import com.pixelquest.app.quran.ui.reader.ReaderTags
import com.pixelquest.app.quran.ui.reader.ReaderUiState
import com.pixelquest.app.quran.ui.reader.SurahReaderScreen
import com.pixelquest.app.quran.ui.search.AyahResult
import com.pixelquest.app.quran.ui.search.QuranSearchScreen
import com.pixelquest.app.quran.ui.search.QuranSearchUiState
import com.pixelquest.app.quran.ui.search.SearchTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PressStart
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * In Pixel, the app's chrome is in its pixel font, but the Quran's text never is: the Arabic is in
 * Amiri Quran and the translation in its own plain typeface, in the reader and in every list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class QuranFontsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Stand-ins, not the Quran's text.
    private val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, "b")
    private val ayah = Ayah(8, 2, 1, "a", "t", "en.test")

    private fun SemanticsNodeInteraction.fontFamily(): FontFamily? {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        return layouts.first().layoutInput.style.fontFamily
    }

    @Test
    fun theReader_inPixel_setsTheQuranInItsOwnTypefaces() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { SurahReaderScreen(state = ReaderUiState(surah = surah, ayahs = listOf(ayah)), onNavigateBack = {}) }
        }

        assertEquals(AmiriQuran, composeTestRule.onNodeWithTag(ReaderTags.arabic(1), useUnmergedTree = true).fontFamily())
        assertEquals(FontFamily.SansSerif, composeTestRule.onNodeWithTag(ReaderTags.translation(1), useUnmergedTree = true).fontFamily())
    }

    @Test
    fun aBookmarksTranslation_inPixel_isntInThePixelFont() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                BookmarksList(BookmarksUiState(listOf(BookmarkGroup(2, "Al-Baqara", listOf(ayah))), loaded = true), { _, _ -> }, { _, _ -> })
            }
        }

        val font = composeTestRule.onNodeWithTag(BookmarkTags.translation(2, 1), useUnmergedTree = true).fontFamily()
        assertEquals(FontFamily.SansSerif, font)
    }

    @Test
    fun aSearchResultsTranslation_inPixel_isntInThePixelFont() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                QuranSearchScreen("t", QuranSearchUiState("t", ayahs = listOf(AyahResult(ayah, "Al-Baqara"))), {}, {}, {}, { _, _ -> }, focusOnOpen = false)
            }
        }

        val font = composeTestRule.onNodeWithTag(SearchTags.translation(2, 1), useUnmergedTree = true).fontFamily()
        assertEquals(FontFamily.SansSerif, font)
        check(font != PressStart)
    }
}
