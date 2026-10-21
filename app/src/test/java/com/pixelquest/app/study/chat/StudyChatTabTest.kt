package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.ChatUiState
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.study.ui.StudyPartyContent
import com.pixelquest.app.study.ui.StudyPartyKind
import com.pixelquest.app.study.ui.StudyPartyUiState
import com.pixelquest.app.study.ui.StudyTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/** The study party's Chat and Files tabs: in the tab bar, with what the party has said and shared. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyChatTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val party = Party(
        id = "p1", name = "Physics group", leaderId = "aria", maxMembers = 10, milestone = null, milestoneSetAt = null,
        completedAt = null, createdAt = Instant.EPOCH, total = 0, memberCount = 2, myStatus = MemberStatus.ACTIVE, type = PartyType.STUDY
    )

    private fun message(id: String, body: String) =
        ChatMessage(id, "p1", "bram", ChatMessageKind.TEXT, body, null, null, null, Instant.parse("2026-10-21T10:00:00Z"), null)

    @Test
    fun theChatTab_showsTheMessages_andFilesShowsWhatsShared() {
        var tab = PartyTab.THIS_WEEK
        val chat = ChatUiState(
            loaded = true,
            rows = listOf(ChatMessageRow(message("m1", "Anyone done Q3?"), mine = false, senderName = "bram")),
            files = listOf(ChatFile("f1", "p1", "bram", "Past paper.pdf", "application/pdf", 1200, null, ChatFileStatus.READY, null, false, Instant.EPOCH, null)),
            today = LocalDate.of(2026, 10, 21)
        )
        val tabState = androidx.compose.runtime.mutableStateOf(tab)
        composeTestRule.setContent {
            PixelQuestTheme {
                StudyPartyContent(
                    state = StudyPartyUiState(party = party, tab = tabState.value, tabs = StudyPartyKind.tabs, loaded = true),
                    onNavigateBack = {}, onOpenInfo = {}, onSelectTab = { tabState.value = it },
                    chatState = chat
                )
            }
        }
        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.CHAT)).performScrollTo().performClick()
        assertEquals(PartyTab.CHAT, tabState.value)
        composeTestRule.onNodeWithTag(StudyChatTags.PANE).assertExists()
        composeTestRule.onNodeWithText("Anyone done Q3?").assertExists()

        composeTestRule.onNodeWithTag(StudyTags.tabButton(PartyTab.FILES)).performScrollTo().performClick()
        composeTestRule.onNodeWithText("Past paper.pdf").assertExists()
    }

    @Test
    fun anEmptyChat_saysHowItStarts() {
        composeTestRule.setContent {
            PixelQuestTheme {
                StudyPartyContent(
                    state = StudyPartyUiState(party = party, tab = PartyTab.CHAT, tabs = StudyPartyKind.tabs, loaded = true),
                    onNavigateBack = {}, onOpenInfo = {}, chatState = ChatUiState(loaded = true)
                )
            }
        }
        composeTestRule.onNodeWithTag(StudyChatTags.EMPTY).assertExists()
    }
}
