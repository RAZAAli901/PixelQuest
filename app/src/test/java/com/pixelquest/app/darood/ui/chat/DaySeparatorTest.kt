package com.pixelquest.app.darood.ui.chat

import com.pixelquest.app.ui.chat.DaySeparator
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import java.time.ZoneId
import java.util.Locale

/** Each day of the chat headed once, in the reader's own time zone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class DaySeparatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var next = 1L

    private fun entry(user: String, at: String) =
        DaroodEntry(next, "c${next++}", "p1", user, 100, LocalDate.of(2026, 10, 19), "UTC", Instant.parse(at))

    private val lateAndEarly = listOf(
        entry("bram-id", "2026-10-18T22:00:00Z"),
        entry("bram-id", "2026-10-18T23:30:00Z"),
        entry("bram-id", "2026-10-19T00:30:00Z")
    )

    private fun shape(items: List<ChatItem>) = items.map {
        when (it) {
            is DayItem -> "day ${it.day}"
            is EntryItem -> if (it.bubble.showName) "named" else "bubble"
            is EventItem -> "event"
        }
    }

    @Test
    fun eachDay_isHeadedOnce_andANewDayNamesTheSenderAgain() {
        assertEquals(
            listOf("day 2026-10-18", "named", "bubble", "day 2026-10-19", "named"),
            shape(chatItemsOf(lateAndEarly, me = "aria-id", zone = ZoneId.of("UTC")))
        )
    }

    @Test
    fun theDays_followTheReadersClock() {
        // In Karachi (UTC+5) all three fall on the 19th: one day, one run.
        assertEquals(
            listOf("day 2026-10-19", "named", "bubble", "bubble"),
            shape(chatItemsOf(lateAndEarly, me = "aria-id", zone = ZoneId.of("Asia/Karachi")))
        )
    }

    @Test
    fun theKeys_stayTheSame_andDontClash() {
        val items = chatItemsOf(lateAndEarly, me = null, zone = ZoneId.of("UTC"))
        assertEquals(items.size, items.map { it.key }.toSet().size)
        assertEquals(items.map { it.key }, chatItemsOf(lateAndEarly, me = null, zone = ZoneId.of("UTC")).map { it.key })
        assertEquals(emptyList<ChatItem>(), chatItemsOf(emptyList(), me = null, zone = ZoneId.of("UTC")))
    }

    @Test
    fun theHeadings_sayTodayYesterday_orTheDate() {
        val default = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val today = LocalDate.of(2026, 10, 19)
            composeTestRule.setContent {
                PixelQuestTheme(themeMode = ThemeMode.Comic) {
                    Column {
                        DaySeparator(day = today, today = today)
                        DaySeparator(day = today.minusDays(1), today = today)
                        DaySeparator(day = LocalDate.of(2026, 10, 10), today = today)
                    }
                }
            }
            composeTestRule.onNodeWithText("Today").assertIsDisplayed()
            composeTestRule.onNodeWithText("Yesterday").assertIsDisplayed()
            composeTestRule.onNodeWithText("Oct 10, 2026").assertIsDisplayed()
        } finally {
            Locale.setDefault(default)
        }
    }
}
