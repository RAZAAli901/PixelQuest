package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** At a surah's end, the way to the one before and the one after; only once it's all loaded. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class SurahNeighboursTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun surah(n: Int, name: String) = Surah(n, "ـ", name, "M$n", Revelation.MECCAN, 3, 1, "b")
    private val ayahs = (1..3).map { Ayah(it, 2, it, "ـ$it", "t$it", "en.test") }
    private val opened = mutableListOf<Int>()

    private fun show(state: ReaderUiState) {
        composeTestRule.setContent {
            PixelQuestTheme { SurahReaderScreen(state = state, onNavigateBack = {}, onOpenSurah = { opened += it }) }
        }
    }

    private fun count(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun atTheEnd_bothNeighbours_byName() {
        show(ReaderUiState(surah = surah(2, "Al-Baqara"), previous = surah(1, "Al-Faatiha"), next = surah(3, "Aal-i-Imraan"), ayahs = ayahs, hasMore = false))

        composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToNode(hasTestTag(ReaderTags.NEXT))
        composeTestRule.onNodeWithText("Aal-i-Imraan ▶", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.NEXT).performClick()
        composeTestRule.onNodeWithTag(ReaderTags.PREVIOUS).performClick()

        assertEquals(listOf(3, 1), opened)
    }

    @Test
    fun alFatihah_hasNoPrevious_andAnNas_noNext() {
        show(ReaderUiState(surah = surah(1, "Al-Faatiha"), next = surah(2, "Al-Baqara"), ayahs = ayahs, hasMore = false))
        assertEquals(0, count(ReaderTags.PREVIOUS))
        assertEquals(1, count(ReaderTags.NEXT))
    }

    @Test
    fun beforeTheEndIsLoaded_noNeighbours() {
        show(ReaderUiState(surah = surah(2, "Al-Baqara"), previous = surah(1, "A"), next = surah(3, "B"), ayahs = ayahs, hasMore = true))

        assertEquals(0, count(ReaderTags.NEXT))
    }
}
