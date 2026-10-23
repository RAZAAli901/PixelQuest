package com.pixelquest.app.darood.ui.chat

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.darood.domain.MemberStats
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.chat.bubbleTime
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** A finished party's card: reached, pooled, and when, for good. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class CelebrationCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val started = Instant.parse("2026-10-07T09:00:00Z")
    private val finished = Instant.parse("2026-10-19T15:05:00Z")

    @Test
    fun theCard_saysItsReached_pooled_andWhen() {
        val default = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            composeTestRule.setContent {
                PixelQuestTheme(themeMode = ThemeMode.Comic) {
                    CelebrationCard(total = 10_240, milestone = 10_000, completedAt = finished, startedAt = started, posters = 5, zone = ZoneId.of("UTC"))
                }
            }
            composeTestRule.onNodeWithText("🎉 Milestone reached!").assertIsDisplayed()
            composeTestRule.onNodeWithText("Together, 5 of you added 10,240. The aim was 10,000.").assertIsDisplayed()
            val time = bubbleTime(finished, ZoneId.of("UTC"))
            composeTestRule.onNodeWithText("Reached Oct 19, 2026 at $time, 12 days after the circle began.").assertIsDisplayed()
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun reachedTheDayItBegan_saysSo() {
        val default = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            composeTestRule.setContent {
                PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                    CelebrationCard(total = 313, milestone = 300, completedAt = finished, startedAt = finished.minusSeconds(3_600), posters = 1, zone = ZoneId.of("UTC"))
                }
            }
            composeTestRule.onNodeWithText("Together, 1 of you added 313. The aim was 300.").assertIsDisplayed()
            composeTestRule.onNodeWithText("the same day the circle began.", substring = true).assertIsDisplayed()
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun aFinishedParty_showsTheCard_inPlaceOfTheField() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PartyChatScreen(
                    partyName = "Friday circle", onNavigateBack = {}, onOpenInfo = {}, canPost = false,
                    milestone = 10_000, total = 10_240, finished = true, completedAt = finished, startedAt = started,
                    myStats = myStatsOf(listOf(MemberStats("p1", "aria-id", "aria_k", 10_240, 3, LocalDate.of(2026, 10, 19))), 10_240, "aria-id"),
                )
            }
        }
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_CELEBRATION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DaroodTestTags.CHAT_INPUT).assertDoesNotExist()
    }
}
