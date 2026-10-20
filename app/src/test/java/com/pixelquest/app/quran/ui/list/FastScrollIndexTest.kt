package com.pixelquest.app.quran.ui.list

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The fast-scroll strip beside the surah list: a mark jumps the list there, and searching hides it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class FastScrollIndexTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val all = (1..114).map { Surah(it, "ـ$it", "Surah $it", "Meaning $it", Revelation.MECCAN, 10, 1, "b") }
    private lateinit var listState: LazyListState

    private fun show(state: SurahListUiState) {
        composeTestRule.setContent {
            listState = rememberLazyListState()
            PixelQuestTheme { SurahList(state = state, onSearch = {}, onOpenSurah = {}, listState = listState) }
        }
    }

    @Test
    fun aMark_jumpsTheListToItsSurah() {
        show(SurahListUiState(surahs = all, loaded = true))

        composeTestRule.onNodeWithTag(FastScrollTags.mark(50)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(49, listState.firstVisibleItemIndex)

        composeTestRule.onNodeWithContentDescription("Jump to surah 100").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QuranListTags.row(100)).assertIsDisplayed()
    }

    @Test
    fun whileSearching_thereIsNoStrip() {
        show(SurahListUiState(query = "1", surahs = all.filter { it.number.toString().startsWith("1") }, loaded = true))

        assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(FastScrollTags.STRIP)).fetchSemanticsNodes().size)
    }
}
