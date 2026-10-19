package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The chat's only input: a number, nothing else. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class NumberInputBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onlyDigitsGetIn_atMostSix_withoutLeadingZeros() {
        val cases = mapOf(
            "300" to "300",
            "10,000 Darood" to "10000",
            "abc" to "",
            "" to "",
            "-5" to "5",
            "1.5e3" to "153",
            "0007" to "7",
            "000" to "0",
            "1234567" to "123456",
            "100000" to "100000",
            "\u06F3\u06F1\u06F3" to "313",
            "\u0663\u0661\u0663" to "313",
            " 3 1 3 " to "313"
        )
        for ((typed, kept) in cases) assertEquals(typed, kept, amountDigits(typed))
    }

    private fun showBar(theme: ThemeMode = ThemeMode.Pixel) = composeTestRule.setContent {
        PixelQuestTheme(themeMode = theme) {
            var value by remember { mutableStateOf("") }
            NumberInputBar(value = value, onValueChange = { value = it })
        }
    }

    private fun field() = composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT)

    @Test
    fun typingLetters_leavesThemOut() {
        showBar()
        field().performTextInput("3a1b3")
        field().assertTextEquals("313", includeEditableText = true)
        field().performTextInput("x")
        field().assertTextEquals("313", includeEditableText = true)
    }

    @Test
    fun aPaste_keepsItsDigits_andASeventhDigitDoesntFit() {
        showBar(ThemeMode.Comic)
        field().performTextReplacement("10,000 Darood")
        field().assertTextEquals("10000", includeEditableText = true)
        field().performTextReplacement("Hello")
        field().assertTextEquals("", includeEditableText = true)
        field().performTextReplacement("1234567")
        field().assertTextEquals("123456", includeEditableText = true)
    }

    @Test
    fun itAsksForTheNumberPad_andTalkBackKnowsWhatItsFor() {
        showBar(ThemeMode.Light)
        field().assertIsDisplayed()
        val node = field().fetchSemanticsNode()
        assertEquals(listOf("How many to add"), node.config[SemanticsProperties.ContentDescription])
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT).assertIsDisplayed()
    }

    @Test
    fun theChat_offersTheField_onlyToThoseWhoMayPost() {
        val canPost = mutableStateOf(true)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, canPost = canPost.value)
            }
        }
        field().assertIsDisplayed()
        // A finished party, or someone no longer in it: nothing to type into.
        canPost.value = false
        field().assertDoesNotExist()
    }

    @Test
    fun emptyAndZero_cantBeSent_andMoreThan100000_isTooMany() {
        assertEquals(DraftCheck.EMPTY, checkDraft(""))
        assertEquals(DraftCheck.ZERO, checkDraft("0"))
        assertEquals(DraftCheck.OK, checkDraft("1"))
        assertEquals(DraftCheck.OK, checkDraft("313"))
        assertEquals(DraftCheck.OK, checkDraft("100000"))
        assertEquals(DraftCheck.TOO_MANY, checkDraft("100001"))
        assertEquals(DraftCheck.TOO_MANY, checkDraft("999999"))
        assertEquals(listOf(false, false, true, false), listOf("", "0", "1", "100001").map { checkDraft(it).canSend })
    }

    @Test
    fun tooMany_isSaidAboveTheField_andGoesWhenItFits() {
        val draft = mutableStateOf("100001")
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, canPost = true, draft = draft.value)
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT_ERROR).assertTextEquals("At most 100,000 in one go.")
        for (fits in listOf("100000", "", "0")) {
            draft.value = fits
            composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT_ERROR).assertDoesNotExist()
        }
    }
}
