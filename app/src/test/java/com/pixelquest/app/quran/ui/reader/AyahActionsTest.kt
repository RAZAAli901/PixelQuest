package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/** A tapped ayah's sheet: its reference, and each action reaching the reader with that ayah. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class AyahActionsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Made-up text, not the Quran's.
    private val ayah = Ayah(262, 2, 255, "ـ", "made-up", "en.test")
    private val done = mutableListOf<Pair<Int, AyahAction>>()

    @Test
    fun theSheet_namesTheAyah_andOffersItsActions() {
        composeTestRule.setContent {
            PixelQuestTheme { AyahActionsContent(ayah = ayah, surahName = "Al-Baqara", bookmarked = false, onAction = { done += ayah.number to it }) }
        }

        composeTestRule.onNodeWithText("Al-Baqara 2:255").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmark").assertIsDisplayed()
        AyahAction.entries.forEach { composeTestRule.onNodeWithTag(ReaderTags.action(it)).performClick() }

        assertEquals(AyahAction.entries.map { 255 to it }, done)
    }

    @Test
    fun aBookmarkedAyah_offersToRemoveIt() {
        composeTestRule.setContent {
            PixelQuestTheme { AyahActionsContent(ayah = ayah, surahName = "Al-Baqara", bookmarked = true, onAction = {}) }
        }

        composeTestRule.onNodeWithText("Remove bookmark").assertIsDisplayed()
    }

    @Test
    fun tappingAnAyah_opensItsSheet() {
        val surah = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 3, 1, null)
        val ayahs = (1..3).map { Ayah(it, 2, it, "ـ$it", "t$it", "en.test") }
        composeTestRule.setContent {
            PixelQuestTheme {
                SurahReaderScreen(
                    state = ReaderUiState(surah = surah, ayahs = ayahs, hasMore = false),
                    onNavigateBack = {},
                    onAyahAction = { a, action -> done += a.number to action }
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderTags.ayah(2)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Al-Baqara 2:2").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderTags.action(AyahAction.COPY_ARABIC)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(2 to AyahAction.COPY_ARABIC), done)
    }
}
