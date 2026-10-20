package com.pixelquest.app.quran.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pixelquest.app.quran.domain.Ayah
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * One ayah in the reader: its text exactly as given, its number in the end-of-ayah sign after it,
 * and its translation below. The texts here are made up, not the Quran's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class AyahItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val ayah = Ayah(262, 2, 255, "ءابت", "made-up translation", "en.test")

    private fun show(mode: DisplayMode) {
        composeTestRule.setContent {
            PixelQuestTheme { AyahItem(ayah = ayah, mode = mode, typography = QuranTypography.of()) }
        }
    }

    private fun text(tag: String) = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        .config.getOrNull(SemanticsProperties.Text)!!.joinToString("") { it.text }

    private fun shown(tag: String) = composeTestRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun theSign_holdsTheNumberInArabicIndicDigits() {
        assertEquals("\u06DD\u0662\u0665\u0665", AyahMarker.of(255))
        assertEquals("\u0661", AyahMarker.arabicDigits(1))
        assertEquals("\u0662\u0668\u0666", AyahMarker.arabicDigits(286))
    }

    @Test
    fun both_theArabicWithItsSign_thenTheTranslation() {
        show(DisplayMode.BOTH)

        val arabic = text(ReaderTags.arabic(255))
        assertTrue("the ayah's text, unchanged, first", arabic.startsWith(ayah.arabic))
        assertEquals(ayah.arabic + "\u00A0" + AyahMarker.of(255), arabic)
        assertEquals("made-up translation", text(ReaderTags.translation(255)))
    }

    @Test
    fun arabicOnly_hasNoTranslation() {
        show(DisplayMode.ARABIC)

        composeTestRule.onNodeWithTag(ReaderTags.arabic(255), useUnmergedTree = true).assertIsDisplayed()
        assertEquals(0, shown(ReaderTags.translation(255)))
    }

    @Test
    fun translationOnly_numbersIt_withoutTheArabic() {
        show(DisplayMode.TRANSLATION)

        assertEquals(0, shown(ReaderTags.arabic(255)))
        composeTestRule.onNodeWithTag(ReaderTags.translation(255), useUnmergedTree = true).assertIsDisplayed()
        assertEquals(1, composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("255"), useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
