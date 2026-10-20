package com.pixelquest.app.quran.ui.list

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Going straight to a surah and ayah: only numbers that are there can be gone to. The dialog's
 * content is tested outside its Dialog, where Robolectric doesn't hang on a text field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class GoToAyahTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Every surah's count, as the metadata has them: Al-Fatihah 7, Al-Baqarah 286, the rest made up. */
    private val counts = (1..114).associateWith { if (it == 1) 7 else if (it == 2) 286 else 10 }

    @Test
    fun theCheck_knowsWhatsThere() {
        assertEquals(GoToCheck.Empty, GoToCheck.of("", "", counts))
        assertEquals(GoToCheck.Go(2, 255), GoToCheck.of("2", "255", counts))
        assertEquals("a blank ayah is the first", GoToCheck.Go(36, 1), GoToCheck.of("36", "", counts))
        assertEquals(GoToCheck.NoSuchSurah(114), GoToCheck.of("115", "1", counts))
        assertEquals(GoToCheck.NoSuchSurah(114), GoToCheck.of("0", "", counts))
        assertEquals(GoToCheck.NoSuchAyah(1, 7), GoToCheck.of("1", "8", counts))
        assertEquals(GoToCheck.NoSuchAyah(1, 7), GoToCheck.of("1", "0", counts))
    }

    @Test
    fun onlyDigits_atMostThree_getIn() {
        assertEquals("255", numberDigits("2a5-5"))
        assertEquals("114", numberDigits("1145"))
        assertEquals("36", numberDigits("٣٦"))
    }

    /** The editable field inside PixelTextField's tagged box. */
    private fun field(tag: String) = composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))

    @Test
    fun goingTo_anAyahThatsThere() {
        val went = mutableListOf<Pair<Int, Int>>()
        composeTestRule.setContent {
            var surah by mutableStateOf("")
            var ayah by mutableStateOf("")
            PixelQuestTheme {
                GoToAyahContent(
                    surah = surah, ayah = ayah, check = GoToCheck.of(surah, ayah, counts),
                    onSurah = { surah = numberDigits(it) }, onAyah = { ayah = numberDigits(it) },
                    onGo = { s, a -> went += s to a }, onCancel = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(GoToTags.GO).assertIsNotEnabled()
        field(GoToTags.SURAH).performTextInput("1")
        field(GoToTags.AYAH).performTextInput("9")
        composeTestRule.onNodeWithText("Surah 1 has 7 ayahs").assertIsDisplayed()
        composeTestRule.onNodeWithTag(GoToTags.GO).assertIsNotEnabled()

        field(GoToTags.SURAH).performTextInput("8")
        composeTestRule.onNodeWithTag(GoToTags.GO).assertIsEnabled().performClick()

        assertEquals(listOf(18 to 9), went)
    }
}
