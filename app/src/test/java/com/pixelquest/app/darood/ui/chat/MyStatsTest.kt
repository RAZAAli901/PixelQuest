package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The player's part of a party: total, share and place. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class MyStatsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun stat(user: String, total: Long) = MemberStats("p1", user, user.removeSuffix("-id"), total, 1, null)

    private val stats = listOf(stat("cleo-id", 5_000), stat("aria-id", 2_450), stat("bram-id", 2_450), stat("dara-id", 100))

    @Test
    fun myTotal_myShare_andMyPlace_withTiesSharingAPlace() {
        assertEquals(MyStats(total = 2_450, share = 2_450.0 / 10_000, place = 2, posters = 4), myStatsOf(stats, 10_000, me = "aria-id"))
        assertEquals("level with Aria: the same place", 2, myStatsOf(stats, 10_000, me = "bram-id").place)
        assertEquals("after two at 2,450: fourth, not third", 4, myStatsOf(stats, 10_000, me = "dara-id").place)
        assertEquals(1, myStatsOf(stats, 10_000, me = "cleo-id").place)
    }

    @Test
    fun beforePosting_thereIsNoPlace_andNoShareOfNothing() {
        assertEquals(MyStats(total = 0, share = 0.0, place = null, posters = 4), myStatsOf(stats, 10_000, me = "eli-id"))
        assertEquals(MyStats(total = 0, share = null, place = null, posters = 0), myStatsOf(emptyList(), 0, me = "aria-id"))
        assertEquals(null, myStatsOf(stats, 10_000, me = null).place)
    }

    @Test
    fun theSheet_opensFromTheTopBar_withTheNumbers() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                PartyChatScreen(partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, myStats = myStatsOf(stats, 10_000, "aria-id"))
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_MY_STATS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_STATS).performClick()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_MY_STATS).assertIsDisplayed()
        composeTestRule.onNodeWithText("2,450", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("25%", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("2 of 4", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aLittleShare_saysUnder1Percent_andNothingYet_saysHowToStart() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(shareText(0.004))
                    androidx.compose.material3.Text(shareText(0.0))
                }
            }
        }
        composeTestRule.onNodeWithText("under 1%").assertExists()
        composeTestRule.onNodeWithText("0%").assertExists()
    }

    @Test
    fun theSheet_saysHowSteady_onceTheDaysAreKnown() {
        val today = java.time.LocalDate.of(2026, 10, 20)
        val steady = com.pixelquest.app.darood.domain.Consistency.of(
            mapOf(today to 300L, today.minusDays(1) to 100L, today.minusDays(2) to 50L, today.minusDays(10) to 10L), today
        )
        var opened = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatScreen(
                    partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {},
                    myStats = myStatsOf(stats, 10_000, "aria-id"), consistency = steady, onOpenStats = { opened++ }
                )
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_STATS).performClick()
        assertEquals("opening asks for the days", 1, opened)
        composeTestRule.onNodeWithText("4/30", useUnmergedTree = true).assertExists()
        // Three days in a row now, and that's the best run too.
        composeTestRule.onNode(androidx.compose.ui.test.hasText("In a row") and androidx.compose.ui.test.hasText("3")).assertExists()
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Best run") and androidx.compose.ui.test.hasText("3")).assertExists()
        composeTestRule.onNodeWithText("115", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theCalendar_fillsTheDaysWithAnEntry_andSaysHowMany() {
        val today = java.time.LocalDate.of(2026, 10, 20)
        val steady = com.pixelquest.app.darood.domain.Consistency.of(
            mapOf(today to 300L, today.minusDays(3) to 100L, today.minusDays(29) to 50L, today.minusDays(30) to 10L), today
        )
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) { ConsistencyCalendar(steady) }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_CALENDAR)
            .assertIsDisplayed()
            .assert(androidx.compose.ui.test.hasContentDescription("Added on 3 of the last 30 days"))
        val posted = listOf(today, today.minusDays(3), today.minusDays(29)).map { it.toString() }
        for (day in steady.window) {
            val tag = DaroodTestTags.calendarDay(day.toString(), day.toString() in posted)
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.calendarDay(today.minusDays(30).toString(), true), useUnmergedTree = true).assertDoesNotExist()
    }
}
