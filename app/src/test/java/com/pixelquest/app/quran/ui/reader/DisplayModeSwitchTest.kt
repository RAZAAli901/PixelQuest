package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reader's display-mode switch: Both by default, and each choice shows what it says. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class DisplayModeSwitchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Made-up text, not the Quran's.
    private val surah = Surah(112, "ـ", "Al-Ikhlaas", "Sincerity", Revelation.MECCAN, 4, 30, null)
    private val ayahs = (1..4).map { Ayah(6221 + it, 112, it, "ـ$it", "t$it", "en.test") }

    private fun count(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun show(mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            var state by mutableStateOf(ReaderUiState(surah = surah, ayahs = ayahs, hasMore = false))
            PixelQuestTheme(themeMode = mode) {
                SurahReaderScreen(state = state, onNavigateBack = {}, onDisplayMode = { state = state.copy(settings = state.settings.copy(displayMode = it)) })
            }
        }
    }

    @Test
    fun both_isTheDefault_showingArabicAndTranslation() {
        show()

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.BOTH)).assertIsSelected()
        assertEquals(4, count(ReaderTags.arabic(1)) + count(ReaderTags.arabic(2)) + count(ReaderTags.arabic(3)) + count(ReaderTags.arabic(4)))
        assertEquals(1, count(ReaderTags.translation(1)))
    }

    @Test
    fun arabic_hidesTheTranslation() {
        show(ThemeMode.Comic)

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.ARABIC)).performClick()

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.ARABIC)).assertIsSelected()
        assertEquals(1, count(ReaderTags.arabic(1)))
        assertEquals(0, count(ReaderTags.translation(1)))
    }

    @Test
    fun translation_hidesTheArabic() {
        show(ThemeMode.Light)

        composeTestRule.onNodeWithTag(ReaderTags.mode(DisplayMode.TRANSLATION)).performClick()

        assertEquals(0, count(ReaderTags.arabic(1)))
        assertEquals(1, count(ReaderTags.translation(1)))
    }
}
