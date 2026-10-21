package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.MessageBubbleRow
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** A study chat's bubbles: named once per run, the player's own unnamed, deleted ones kept, refused ones retried. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class MessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun message(id: String, body: String?, sender: String = "bram", deleted: Boolean = false, sync: SyncState? = null) =
        ChatMessage(id, "p1", sender, ChatMessageKind.TEXT, body, null, null, "c-$id", Instant.parse("2026-10-21T10:00:00Z"), if (deleted) Instant.EPOCH else null, sync)

    @Test
    fun aRun_isNamedOnce_andThePlayersOwnIsNeverNamed() {
        composeTestRule.setContent {
            PixelQuestTheme {
                Column {
                    MessageBubbleRow(ChatMessageRow(message("1", "Anyone done Q3?"), mine = false, senderName = "bram", firstOfRun = true, lastOfRun = false))
                    MessageBubbleRow(ChatMessageRow(message("2", "It's the hard one"), mine = false, senderName = "bram", firstOfRun = false))
                    MessageBubbleRow(ChatMessageRow(message("3", "Yes: 42", sender = "aria"), mine = true, senderName = "aria_k"))
                }
            }
        }
        assertEquals(1, composeTestRule.onAllNodesWithText("@bram").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("@aria_k").fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("Yes: 42").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.SENT, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDeletedMessage_keepsItsPlace_withoutItsText() {
        composeTestRule.setContent {
            PixelQuestTheme { MessageBubbleRow(ChatMessageRow(message("1", null, deleted = true), mine = false, senderName = "bram")) }
        }
        composeTestRule.onNodeWithText("Message deleted").assertExists()
    }

    @Test
    fun aRefusedMessage_isRetriedOrDiscarded() {
        val tried = mutableListOf<String>()
        composeTestRule.setContent {
            PixelQuestTheme {
                MessageBubbleRow(
                    ChatMessageRow(message("1", "Spam", sender = "aria", sync = SyncState.FAILED), mine = true, senderName = "aria_k"),
                    onRetry = { tried += "retry $it" }, onDiscard = { tried += "discard $it" }
                )
            }
        }
        composeTestRule.onNodeWithTag(StudyChatTags.FAILED, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.RETRY).performClick()
        composeTestRule.onNodeWithTag(StudyChatTags.DISCARD).performClick()
        assertEquals(listOf("retry c-1", "discard c-1"), tried)
    }
}
