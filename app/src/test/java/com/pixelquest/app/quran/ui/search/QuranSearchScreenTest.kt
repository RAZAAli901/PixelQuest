package com.pixelquest.app.quran.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.list.QuranListTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The search screen: a hint, then surahs and ayahs found, each opening the reader; or "nothing found". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class QuranSearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Stand-ins, not the Quran's text: QuranSearchViewModelTest searches the real one.
    private val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, null)
    private val ayah = Ayah(262, 2, 255, "ـ", "a stand-in about the Throne", "en.test")
    private val opened = mutableListOf<String>()

    private fun show(query: String, state: QuranSearchUiState, mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                QuranSearchScreen(
                    query = query,
                    state = state,
                    onSearch = {},
                    onNavigateBack = {},
                    onOpenSurah = { opened += "surah $it" },
                    onOpenAyah = { s, a -> opened += "$s:$a" },
                    focusOnOpen = false
                )
            }
        }
    }

    @Test
    fun beforeTyping_itSaysWhatCanBeSearched() {
        show("", QuranSearchUiState())

        composeTestRule.onNodeWithTag(SearchTags.NOTE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SearchTags.RESULTS).assertDoesNotExist()
    }

    @Test
    fun results_openTheSurah_orTheAyah() {
        show("throne", QuranSearchUiState("throne", listOf(surah), listOf(AyahResult(ayah, "Al-Baqara"))), ThemeMode.Comic)

        composeTestRule.onNodeWithText("1 SURAH").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 AYAH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Al-Baqara 2:255", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag(SearchTags.ayah(2, 255)).performClick()
        composeTestRule.onNodeWithTag(QuranListTags.row(2)).performClick()

        assertEquals(listOf("2:255", "surah 2"), opened)
    }

    @Test
    fun aCappedSearch_saysItShowsOnlyTheFirst() {
        val two = listOf(AyahResult(ayah, "Al-Baqara"), AyahResult(ayah.copy(id = 263, number = 256), "Al-Baqara"))
        show("allah", QuranSearchUiState("allah", ayahs = two, capped = true), ThemeMode.Comic)

        composeTestRule.onNodeWithText("THE FIRST 2 AYAHS: ADD A WORD TO NARROW IT").assertIsDisplayed()
    }

    @Test
    fun foundByItsArabic_theArabicIsShown() {
        show("x", QuranSearchUiState("x", ayahs = listOf(AyahResult(ayah, "Al-Baqara")), arabic = true))

        composeTestRule.onNodeWithTag(SearchTags.arabic(2, 255), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun foundByItsTranslation_theArabicIsLeftOut() {
        show("x", QuranSearchUiState("x", ayahs = listOf(AyahResult(ayah, "Al-Baqara"))))

        composeTestRule.onNodeWithTag(SearchTags.arabic(2, 255), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun nothingFound_saysSo_onceTheResultsAreForWhatsTyped() {
        show("zzqx", QuranSearchUiState("zzqx"))

        composeTestRule.onNodeWithText("Nothing found for \u201czzqx\u201d").assertIsDisplayed()
    }
}
