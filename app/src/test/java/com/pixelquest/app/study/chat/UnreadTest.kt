package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.party.ui.PartyCard
import com.pixelquest.app.party.ui.PartiesTags
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatLine
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.ChatUiState
import com.pixelquest.app.study.chat.ui.linesOf
import com.pixelquest.app.study.ui.StudyPartyContent
import com.pixelquest.app.study.ui.StudyPartyKind
import com.pixelquest.app.study.ui.StudyPartyUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** What the player hasn't read: a line in the chat where it starts, a count on its tab and on the party's row. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class UnreadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun row(id: String, at: String, mine: Boolean = false) = ChatMessageRow(
        ChatMessage(id, "p1", if (mine) "aria" else "bram", ChatMessageKind.TEXT, "m$id", null, null, null, Instant.parse(at), null),
        mine = mine, senderName = null
    )

    private val party = Party(
        id = "p1", name = "Physics group", leaderId = "aria", maxMembers = 10, milestone = null, milestoneSetAt = null,
        completedAt = null, createdAt = Instant.EPOCH, total = 0, memberCount = 2, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY
    )

    @Test
    fun theUnreadLine_startsAtTheFirstOfOthersAfterTheMark() {
        val rows = listOf(row("1", "2026-10-21T09:00:00Z"), row("2", "2026-10-21T11:00:00Z", mine = true), row("3", "2026-10-21T12:00:00Z"))
        val lines = linesOf(rows, ZoneId.of("UTC"), readAt = Instant.parse("2026-10-21T10:00:00Z"))
        assertEquals(listOf("day", "m1", "m2", "unread", "m3"), lines.map {
            when (it) {
                is ChatLine.Day -> "day"
                is ChatLine.Message -> it.row.message.body!!
                ChatLine.Unread -> "unread"
            }
        })
        assertEquals("never opened: no line", 0, linesOf(rows, ZoneId.of("UTC"), readAt = null).count { it == ChatLine.Unread })
        assertEquals("all read: no line", 0, linesOf(rows, ZoneId.of("UTC"), readAt = Instant.parse("2026-10-21T13:00:00Z")).count { it == ChatLine.Unread })
    }

    @Test
    fun theChatTab_countsWhatsUnread() {
        composeTestRule.setContent {
            PixelQuestTheme {
                StudyPartyContent(
                    state = StudyPartyUiState(party = party, tab = PartyTab.THIS_WEEK, tabs = StudyPartyKind.tabs, loaded = true),
                    onNavigateBack = {}, onOpenInfo = {}, chatState = ChatUiState(loaded = true, unread = 3)
                )
            }
        }
        composeTestRule.onNodeWithText("Chat 3").performScrollTo().assertExists()
        composeTestRule.onNodeWithContentDescription("Chat, 3 unread messages", useUnmergedTree = true).assertExists()
    }

    @Test
    fun thePartysRow_countsWhatsUnread() {
        composeTestRule.setContent { PixelQuestTheme { PartyCard(party, onClick = {}, unread = 120) } }
        composeTestRule.onNodeWithTag(PartiesTags.UNREAD, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }
}
