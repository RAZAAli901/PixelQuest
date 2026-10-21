package com.pixelquest.app.quran.ui.list

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.quran.domain.Juz
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.QuranHomeScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Juz tab: the 30 juz with where each starts, a tap opening the reader there. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class JuzListTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Where Tanzil's metadata starts the first three juz.
    private val juzs = listOf(Juz(1, 1, 1, "Al-Faatiha"), Juz(2, 2, 142, "Al-Baqara"), Juz(3, 2, 253, "Al-Baqara"))
    private val state = SurahListUiState(
        surahs = listOf(Surah(1, "ـ", "Al-Faatiha", "The Opening", Revelation.MECCAN, 7, 1, null)),
        loaded = true,
        juzs = juzs
    )
    private val opened = mutableListOf<Pair<Int, Int>>()

    private fun show(mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                QuranHomeScreen(onNavigateBack = {}, list = state, onOpenAyah = { s, a -> opened += s to a })
            }
        }
    }

    @Test
    fun theSurahsTab_comesFirst_thenTheJuzTabShowsTheJuz() {
        show()
        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.SURAHS)).assertIsSelected()

        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.JUZ)).performClick()

        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.JUZ)).assertIsSelected()
        composeTestRule.onNodeWithText("Juz 2", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("From Al-Baqara 2:142", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aJuz_opensTheReaderWhereItStarts() {
        show(ThemeMode.Comic)
        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.JUZ)).performClick()

        composeTestRule.onNodeWithTag(QuranListTags.juz(3)).performClick()

        assertEquals(listOf(2 to 253), opened)
    }

    @Test
    fun aJuz_isOneButton_readAsASentence() {
        show(ThemeMode.Comic)
        composeTestRule.onNodeWithTag(QuranListTags.tab(QuranTab.JUZ)).performClick()

        val spoken = composeTestRule.onNodeWithTag(QuranListTags.juz(2)).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription)!!.single()
        assertEquals("Juz 2, from Al-Baqara, ayah 142", spoken)
    }
}
