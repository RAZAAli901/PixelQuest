package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reader's frame: its top bar, its list, loading more near the end, and going to its target. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class SurahReaderScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Made-up text ("2:1"), not the Quran's.
    private val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 1, "b")
    private fun ayahs(count: Int) = (1..count).map { Ayah(it, 2, it, "2:$it", "t$it", "en.test") }

    private lateinit var listState: LazyListState
    private var loadMore = 0
    private var back = 0
    private var settings = 0
    private val scrolled = mutableListOf<Int>()
    private var targetShown = 0

    private fun show(state: ReaderUiState) {
        composeTestRule.setContent {
            listState = rememberLazyListState()
            PixelQuestTheme {
                SurahReaderScreen(
                    state = state,
                    onNavigateBack = { back++ },
                    onLoadMore = { loadMore++ },
                    onScrolled = { scrolled += it },
                    onTargetShown = { targetShown++ },
                    onOpenSettings = { settings++ },
                    listState = listState
                )
            }
        }
    }

    @Test
    fun theTopBar_namesTheSurah_withBackAndSettings() {
        show(ReaderUiState(surah = surah, ayahs = ayahs(50)))

        composeTestRule.onNodeWithText("Al-Baqara", substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("The Cow · 286 ayahs").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithTag(ReaderTags.SETTINGS).performClick()
        assertEquals(1, back)
        assertEquals(1, settings)
    }

    @Test
    fun nearingTheEnd_asksForMore() {
        show(ReaderUiState(surah = surah, ayahs = ayahs(50), hasMore = true))
        val before = loadMore

        composeTestRule.onNodeWithTag(ReaderTags.LIST).performScrollToIndex(45)
        composeTestRule.waitForIdle()

        assertTrue(loadMore > before)
        assertTrue("it says where the player is", scrolled.isNotEmpty() && scrolled.last() > 1)
    }

    @Test
    fun theBismillah_comesFirst_asTheSurahGivesIt() {
        show(ReaderUiState(surah = surah.copy(bismillah = "ـبسم"), ayahs = ayahs(5)))

        val shown = composeTestRule.onNodeWithTag(ReaderTags.BISMILLAH).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString("") { it.text }
        assertEquals("ـبسم", shown)
        assertEquals("one header", 1, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag(ReaderTags.BISMILLAH)).fetchSemanticsNodes().size)
    }

    @Test
    fun alFatihahAndAtTawbah_haveNoHeader() {
        for (number in listOf(1, 9)) {
            assertEquals(0, headerCount(surah.copy(number = number, bismillah = null)))
        }
        assertEquals(1, headerCount(surah))
    }

    @Test
    fun aTarget_isBroughtIntoView_onceLoaded_pastTheBismillah() {
        show(ReaderUiState(surah = surah, ayahs = ayahs(286), hasMore = false, target = 255))
        composeTestRule.waitForIdle()

        assertEquals("the Bismillah, then ayahs 1 to 254 before it", 255, listState.firstVisibleItemIndex)
        assertEquals(1, targetShown)
        composeTestRule.onNodeWithTag(ReaderTags.ayah(255)).assertIsDisplayed()
    }
}
