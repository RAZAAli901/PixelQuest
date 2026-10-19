package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.pixelquest.app.darood.domain.DaroodEntry
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

/** A run of one sender's bubbles: stacked, and named once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatItemsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var next = 1L

    private fun entry(user: String?, amount: Int = 100) = DaroodEntry(
        next, "c${next++}", "p1", user, amount, LocalDate.of(2026, 10, 19), "UTC", Instant.parse("2026-10-19T10:00:00Z")
    )

    private val usernames = mapOf("aria-id" to "aria_k", "bram-id" to "bram", "cleo-id" to "cleo_r")

    @Test
    fun oneSendersEntriesInARow_makeARun_namedAtItsStart() {
        val entries = listOf(entry("bram-id"), entry("bram-id"), entry("bram-id"), entry("aria-id"), entry("aria-id"), entry("bram-id"), entry("cleo-id"))
        val bubbles = bubblesOf(entries, me = "aria-id")

        assertEquals(listOf(true, false, false, true, false, true, true), bubbles.map { it.firstOfRun })
        assertEquals(listOf(false, false, true, false, true, true, true), bubbles.map { it.lastOfRun })
        assertEquals(listOf(true, false, false, false, false, true, true), bubbles.map { it.showName })
        assertEquals(listOf(false, false, false, true, true, false, false), bubbles.map { it.mine })
    }

    @Test
    fun unknownPosters_neverJoinARun() {
        val bubbles = bubblesOf(listOf(entry(null), entry(null), entry("bram-id")), me = "aria-id")
        assertEquals(listOf(true, true, true), bubbles.map { it.showName })
        assertEquals(listOf(false, false, false), bubbles.map { it.mine })
    }

    @Test
    fun signedOut_nothingIsMine() {
        assertEquals(listOf(false), bubblesOf(listOf(entry("aria-id")), me = null).map { it.mine })
    }

    @Test
    fun onScreen_eachRunShowsItsNameOnce() {
        val bubbles = bubblesOf(listOf(entry("bram-id"), entry("bram-id"), entry("bram-id"), entry("aria-id"), entry("bram-id")), me = "aria-id")
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                Column { bubbles.forEach { ChatEntryRow(item = it, sender = usernames[it.entry.userId]) } }
            }
        }
        // Two runs of Bram's: his name twice, not four times; Aria's own, never.
        composeTestRule.onAllNodesWithText("@bram", useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onAllNodesWithText("@aria_k", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("+100", useUnmergedTree = true).assertCountEquals(5)
    }
}
