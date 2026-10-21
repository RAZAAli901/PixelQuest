package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.BubbleColors
import com.pixelquest.app.ui.chat.bubbleColors
import com.pixelquest.app.ui.chat.bubbleTime
import com.pixelquest.app.ui.chat.memberColor
import android.app.Application
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** An entry's bubble: who, how many, and when. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class EntryBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val at = Instant.parse("2026-10-19T10:05:00Z")

    private fun entry(amount: Int, clientId: String? = "c1", id: Long? = null) =
        DaroodEntry(id, clientId, "p1", "bram-id", amount, LocalDate.of(2026, 10, 19), "UTC", at)

    @Test
    fun theTime_isThePhonesClock_inItsLanguage() {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            // Newer JDKs put a narrow no-break space before AM; either reads the same.
            assertEquals("10:05 AM", bubbleTime(at, ZoneId.of("UTC")).replace('\u202F', ' '))
            assertEquals("3:05 PM", bubbleTime(at, ZoneId.of("Asia/Karachi")).replace('\u202F', ' '))
            Locale.setDefault(Locale.GERMANY)
            assertEquals("15:05", bubbleTime(at, ZoneId.of("Asia/Karachi")))
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun aBubble_namesItsSender_andShowsTheAmount_andTheTime() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { EntryBubble(entry(2450), sender = "bram") }
        }
        composeTestRule.onNodeWithText("@bram", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("+2,450", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(bubbleTime(at), useUnmergedTree = true).assertIsDisplayed()
        // One bubble, read as one: TalkBack's own wording comes later (D104).
        composeTestRule.onNodeWithTag(DaroodTestTags.bubble("c1")).assertIsDisplayed()
    }

    @Test
    fun anUnknownSender_isSomeone_andTheNameCanBeLeftOff() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                androidx.compose.foundation.layout.Column {
                    EntryBubble(entry(300, clientId = null, id = 7), sender = null)
                    EntryBubble(entry(11, clientId = "c2"), sender = "bram", showName = false)
                }
            }
        }
        composeTestRule.onNodeWithText("Someone", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("+300", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.bubble("7")).assertIsDisplayed()
        composeTestRule.onNodeWithText("+11", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("@bram", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun mine_areOnTheRight_inTheAccent_andUnnamed_othersOnTheLeft_named() {
        var mine: BubbleColors? = null
        var theirs: BubbleColors? = null
        var accent = androidx.compose.ui.graphics.Color.Unspecified
        var surface = androidx.compose.ui.graphics.Color.Unspecified
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                mine = bubbleColors(mine = true)
                theirs = bubbleColors(mine = false)
                accent = com.pixelquest.app.ui.theme.PixelTheme.colors.primary
                surface = com.pixelquest.app.ui.theme.PixelTheme.colors.surface
                androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.testTag("chat")) {
                    ChatEntryRow(entry(300, clientId = "mine"), sender = "aria_k", mine = true)
                    ChatEntryRow(entry(11, clientId = "theirs"), sender = "bram", mine = false, memberColor = androidx.compose.ui.graphics.Color(0xFF1E88E5))
                }
            }
        }
        val chat = composeTestRule.onNodeWithTag("chat").fetchSemanticsNode().boundsInRoot
        val myBubble = composeTestRule.onNodeWithTag(DaroodTestTags.bubble("mine")).fetchSemanticsNode().boundsInRoot
        val theirBubble = composeTestRule.onNodeWithTag(DaroodTestTags.bubble("theirs")).fetchSemanticsNode().boundsInRoot
        assertEquals("mine against the right edge", chat.right, myBubble.right, 1f)
        assertEquals("theirs against the left edge", chat.left, theirBubble.left, 1f)

        assertEquals(accent, mine!!.container)
        assertEquals(surface, theirs!!.container)
        composeTestRule.onNodeWithText("@aria_k", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("@bram", useUnmergedTree = true).assertIsDisplayed()
    }
}
