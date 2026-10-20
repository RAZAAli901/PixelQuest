package com.pixelquest.app.quran.ui.list

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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

/** The surah list's rows: what each shows, how TalkBack reads it, and that it opens its surah. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class SurahListTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Tanzil's metadata for these surahs (names are data, not ayahs). The Arabic names are left as
    // stand-ins here; SurahListViewModelTest uses the real ones.
    private val surahs = listOf(
        Surah(1, "ـ1", "Al-Faatiha", "The Opening", Revelation.MECCAN, 7, 1, null),
        Surah(2, "ـ2", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, "b"),
        Surah(112, "ـ112", "Al-Ikhlaas", "Sincerity", Revelation.MECCAN, 4, 30, "b")
    ) + (3..111).map { Surah(it, "ـ$it", "Surah $it", "Meaning $it", Revelation.MECCAN, 10, 1, "b") }

    private val opened = mutableListOf<Int>()

    private fun show(state: SurahListUiState, mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                SurahList(state = state, onSearch = {}, onOpenSurah = { opened += it })
            }
        }
    }

    @Test
    fun aRow_showsItsNameMeaningPlaceAndCount() {
        show(SurahListUiState(surahs = surahs, loaded = true))

        composeTestRule.onNodeWithText("Al-Baqara", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("The Cow", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Medinan · 286 ayahs", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aRow_isOneButton_readInEnglish() {
        show(SurahListUiState(surahs = surahs, loaded = true))

        val spoken = composeTestRule.onNodeWithTag(QuranListTags.row(2)).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)!!.single()
        assertEquals("2. Al-Baqara, The Cow. Medinan, 286 ayahs", spoken)
        val one = composeTestRule.onNodeWithTag(QuranListTags.row(1)).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)!!.single()
        assertEquals("1. Al-Faatiha, The Opening. Meccan, 7 ayahs", one)
    }

    @Test
    fun tappingARow_opensItsSurah() {
        show(SurahListUiState(surahs = surahs, loaded = true))

        composeTestRule.onNodeWithTag(QuranListTags.row(2)).performClick()
        composeTestRule.onNodeWithTag(QuranListTags.LIST).performScrollToIndex(surahs.indexOfFirst { it.number == 112 })
        composeTestRule.onNodeWithTag(QuranListTags.row(112)).performClick()

        assertEquals(listOf(2, 112), opened)
    }

    @Test
    fun aSearchThatFindsNothing_saysSo() {
        show(SurahListUiState(query = "zz", surahs = emptyList(), loaded = true), ThemeMode.Comic)

        composeTestRule.onNodeWithText("No surah matches that").assertIsDisplayed()
    }
}
