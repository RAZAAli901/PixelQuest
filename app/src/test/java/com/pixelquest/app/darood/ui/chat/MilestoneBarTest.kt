package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The milestone, pinned under the chat's top bar: open, or folded to a slim bar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class MilestoneBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(milestone: Long?, total: Long = 2_450, finished: Boolean = false, theme: ThemeMode = ThemeMode.Pixel) =
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = theme) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, milestone = milestone, total = total, finished = finished)
            }
        }

    private fun bar() = composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_MILESTONE)

    private fun said() = bar().fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()

    private fun state() = bar().fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    @Test
    fun open_itSaysHowFar_andATapFoldsItToASlimBar_andBack() {
        show(milestone = 10_000)
        composeTestRule.onNodeWithText("Milestone: 2,450 of 10,000 (24%)", useUnmergedTree = true).assertIsDisplayed()
        assertEquals("Open", state())
        val openHeight = bar().fetchSemanticsNode().size.height

        bar().performClick()
        composeTestRule.onNodeWithText("Milestone: 2,450 of 10,000 (24%)", useUnmergedTree = true).assertDoesNotExist()
        bar().assertIsDisplayed()
        assertEquals("Folded", state())
        assertEquals("TalkBack still hears how far", "Milestone: 2,450 of 10,000 (24%)", said())
        assert(bar().fetchSemanticsNode().size.height < openHeight) { "folded is slimmer" }

        bar().performClick()
        assertEquals("Open", state())
    }

    @Test
    fun reached_itSaysSo() {
        show(milestone = 10_000, total = 10_200, finished = true, theme = ThemeMode.Comic)
        assertEquals("Milestone reached: 10,000", said())
    }

    @Test
    fun noMilestone_noBar() {
        show(milestone = null)
        bar().assertDoesNotExist()
    }
}
