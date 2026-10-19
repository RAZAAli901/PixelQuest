package com.pixelquest.app.darood.ui.chat

import android.app.Application
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
}
