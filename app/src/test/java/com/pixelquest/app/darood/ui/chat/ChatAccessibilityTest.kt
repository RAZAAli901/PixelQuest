package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.bubbleTime
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.pixelquest.app.darood.domain.DaroodEntry
import com.pixelquest.app.darood.domain.DayTotals
import com.pixelquest.app.darood.domain.EntryState
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The chat for TalkBack, and with the phone's largest text. Drawn with Robolectric's native
 * graphics: its default ones don't measure text, so nothing would ever look cut off.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h740dp")
class ChatAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val at = Instant.parse("2026-10-19T09:01:00Z")

    private fun entry(key: String, user: String?, amount: Int, state: EntryState = EntryState.SYNCED) =
        DaroodEntry(null, key, "p1", user, amount, LocalDate.of(2026, 10, 19), "UTC", at, state)

    @Test
    fun eachBubble_isOneSentence_whoHowManyWhen_andForMineWhetherItsSent() {
        val time = bubbleTime(at)
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                Column {
                    EntryBubble(entry("theirs", "bram-id", 300), sender = "bram")
                    EntryBubble(entry("someone", null, 11), sender = null)
                    EntryBubble(entry("sending", "aria-id", 313, EntryState.PENDING), sender = "aria_k", mine = true)
                    EntryBubble(entry("sent", "aria-id", 2450), sender = "aria_k", mine = true)
                    EntryBubble(entry("refused", "aria-id", 33, EntryState.FAILED), sender = "aria_k", mine = true)
                }
            }
        }
        fun bubble(key: String) = composeTestRule.onNodeWithTag(DaroodTestTags.bubble(key))
        bubble("theirs").assertContentDescriptionEquals("bram added 300 at $time")
        bubble("someone").assertContentDescriptionEquals("Someone added 11 at $time")
        bubble("sending").assertContentDescriptionEquals("You added 313 at $time. Sending.")
        bubble("sent").assertContentDescriptionEquals("You added 2,450 at $time. Sent.")
        bubble("refused").assertContentDescriptionEquals("You added 33 at $time. Not sent.")
    }

    /** The text as it was laid out, to see whether any of it was cut off. */
    private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.first()
    }

    @Test
    fun atTwiceTheTextSize_nothingIsCutOff_andEverythingCanBeReached() {
        val entries = listOf(
            entry("big", "bram-id", 100_000),
            entry("mine", "aria-id", 100_000, EntryState.PENDING)
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                PixelQuestTheme(themeMode = ThemeMode.Comic) {
                    PartyChatScreen(
                        partyName = "Friday night circle", onNavigateBack = {}, onOpenInfo = {}, memberCount = 12,
                        items = chatItemsOf(entries, "aria-id", ZoneId.of("UTC")),
                        usernames = mapOf("bram-id" to "bram"), today = LocalDate.of(2026, 10, 19),
                        canPost = true, draft = "100000", todayTotals = DayTotals(100_000, 200_000),
                        milestone = 1_000_000, total = 200_000
                    )
                }
            }
        }
        // The numbers in full: the time goes under them instead.
        for (amount in composeTestRule.onAllNodesWithTextList("+100,000")) {
            assertFalse("+100,000 cut off", amount.cutOff())
        }
        composeTestRule.onNodeWithText("You today: 100,000", useUnmergedTree = true).let { assertFalse("the day's numbers cut off", it.cutOff()) }
        // Every quick chip can be reached, the row scrolling sideways.
        for (amount in QUICK_AMOUNTS) {
            composeTestRule.onNodeWithTag(DaroodTestTags.quickAdd(amount)).performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_SEND).assertIsDisplayed()
    }

    /**
     * Whether the text didn't fit: its natural width wider than the room it got, or wrapped or
     * ellipsized. (This Compose's didOverflowWidth is true whenever a text is narrower than its
     * room, so it can't tell.)
     */
    private fun SemanticsNodeInteraction.cutOff(): Boolean {
        val text = layout()
        return text.multiParagraph.intrinsics.maxIntrinsicWidth > text.size.width + 1 ||
            text.didOverflowHeight ||
            (0 until text.lineCount).any { text.isLineEllipsized(it) }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextList(text: String): List<SemanticsNodeInteraction> {
        val all = onAllNodes(androidx.compose.ui.test.hasText(text), useUnmergedTree = true)
        val count = all.fetchSemanticsNodes().size
        assert(count > 0) { "no $text" }
        return (0 until count).map { all[it] }
    }
}
