package com.pixelquest.app.study.ui

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderKind
import com.pixelquest.app.study.domain.ReminderOccurrence
import com.pixelquest.app.study.domain.ReminderRepeat
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The week's reminders across the top of This week: what each says, and which open to edit. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ReminderBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 21)

    private fun line(id: String, title: String, scope: ReminderScope, start: String, mine: Boolean, audience: ReminderAudience = ReminderAudience.PARTY): ReminderLine {
        val at = Instant.parse(start).atZone(zone)
        val reminder = StudyReminder(
            id = id, partyId = "p", createdBy = if (mine) "aria" else "bram", title = title, kind = ReminderKind.QUIZ, scope = scope,
            dueAt = at.toInstant(), weekStart = null, tz = "UTC", repeat = ReminderRepeat.NONE, audience = audience,
            alertOffsets = null, createdAt = Instant.EPOCH
        )
        return ReminderLine(reminder, ReminderOccurrence(at, at), mine)
    }

    @Test
    fun eachLine_saysWhatAndWhen_andOnlyThePlayersOwnOpen() {
        val opened = mutableListOf<String>()
        val lines = listOf(
            line("a", "Quiz this week", ReminderScope.WEEK, "2026-10-19T00:00:00Z", mine = false),
            line("b", "Lab report", ReminderScope.TIME, "2026-10-22T10:00:00Z", mine = true, audience = ReminderAudience.ME)
        )
        composeTestRule.setContent {
            PixelQuestTheme { ReminderBanner(lines, today, zone, canEdit = { it.mine }, onOpen = { opened += it.reminder.id }) }
        }
        composeTestRule.onNodeWithText("Quiz: Quiz this week").assertExists()
        composeTestRule.onNodeWithText("All week").assertExists()
        // The time is in the phone's own format: only its start is checked.
        composeTestRule.onNodeWithText("Tomorrow, 10", substring = true).assertExists()
        composeTestRule.onNodeWithText("· Just you", substring = true).assertExists()

        composeTestRule.onNodeWithTag(ReminderTags.line("a")).assertHasNoClickAction()
        composeTestRule.onNodeWithTag(ReminderTags.line("b")).assertHasClickAction().performClick()
        assertEquals(listOf("b"), opened)
    }

    @Test
    fun noReminders_noBanner() {
        composeTestRule.setContent { PixelQuestTheme { ReminderBanner(emptyList(), today, zone, canEdit = { true }, onOpen = {}) } }
        composeTestRule.onNodeWithTag(ReminderTags.BANNER).assertDoesNotExist()
    }
}
