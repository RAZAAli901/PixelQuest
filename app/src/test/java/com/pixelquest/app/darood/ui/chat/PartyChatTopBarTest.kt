package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.initialsOf
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The chat's top bar: the party's picture, its name, and how many are in it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PartyChatTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(theme: ThemeMode, members: Int, name: String = "Friday circle") {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = theme, darkTheme = theme != ThemeMode.Light) {
                PartyChatScreen(partyName = name, onNavigateBack = {}, onOpenInfo = {}, memberCount = members)
            }
        }
    }

    @Test
    fun theInitials_areTheFirstLettersOfTheFirstTwoWords() {
        assertEquals("FC", initialsOf("Friday circle"))
        assertEquals("F", initialsOf("family"))
        assertEquals("FN", initialsOf("  friday   night  gathering "))
        assertEquals("\uD83C\uDF19N", initialsOf("\uD83C\uDF19 nights"))
        assertEquals("?", initialsOf("   "))
    }

    @Test
    fun pixel_showsThePicture_theName_andTheMemberCount() {
        show(ThemeMode.Pixel, members = 12)
        composeTestRule.onNodeWithText("Friday circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 members").assertIsDisplayed()
        // The picture is there, but what TalkBack reads (the merged tree) skips it: the name says the same.
        composeTestRule.onNodeWithTag(DaroodTestTags.PARTY_AVATAR, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("FC").assertDoesNotExist()
    }

    @Test
    fun comic_saysOneMember_inItsOwnCapitals() {
        show(ThemeMode.Comic, members = 1)
        composeTestRule.onNodeWithText("FRIDAY CIRCLE").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 member").assertIsDisplayed()
    }

    @Test
    fun light_withTheCountNotKnownYet_showsNoCount() {
        show(ThemeMode.Light, members = 0)
        composeTestRule.onNodeWithText("Friday circle").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 members").assertDoesNotExist()
    }
}
