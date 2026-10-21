package com.pixelquest.app.quran.ui.list

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

/** The Continue reading card: on the Quran screen once the player has read, and back to that place. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class ContinueReadingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val alBaqara = Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 87, null)
    private val opened = mutableListOf<Pair<Int, Int>>()

    private fun show(state: SurahListUiState, mode: ThemeMode = ThemeMode.Pixel) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                QuranHomeScreen(onNavigateBack = {}, list = state, onOpenAyah = { s, a -> opened += s to a })
            }
        }
    }

    @Test
    fun beforeAnyReading_thereIsNoCard() {
        show(SurahListUiState(surahs = listOf(alBaqara), loaded = true))

        composeTestRule.onNodeWithTag(CONTINUE_READING).assertDoesNotExist()
    }

    @Test
    fun theCard_namesThePlace_andOpensTheReaderThere() {
        show(SurahListUiState(surahs = listOf(alBaqara), loaded = true, continueReading = ContinueReading(alBaqara, 255)), ThemeMode.Comic)

        composeTestRule.onNodeWithText("CONTINUE READING", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("AL-BAQARA", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ayah 255 / 286", useUnmergedTree = true).assertIsDisplayed()
        // TalkBack hears the place in words.
        composeTestRule.onNodeWithTag(CONTINUE_READING)
            .assert(androidx.compose.ui.test.hasContentDescription("Continue reading: 2. Al-Baqara, ayah 255"))
        composeTestRule.onNodeWithTag(CONTINUE_READING).assertHasClickAction().performClick()

        assertEquals(listOf(2 to 255), opened)
    }
}
