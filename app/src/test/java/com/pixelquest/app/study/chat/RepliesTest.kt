package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.MessageBubbleRow
import com.pixelquest.app.study.chat.ui.ReplyPreview
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Answering a message: swiped or by TalkBack, quoted in its bubble and over the input. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class RepliesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun message(id: String, body: String?, sender: String = "bram", deleted: Boolean = false) =
        ChatMessage(id, "p1", sender, ChatMessageKind.TEXT, body, null, null, null, Instant.parse("2026-10-21T10:00:00Z"), if (deleted) Instant.EPOCH else null)

    @Test
    fun aSwipeToTheRight_orTalkBacksAction_answersIt() {
        var replies = 0
        composeTestRule.setContent {
            PixelQuestTheme { MessageBubbleRow(ChatMessageRow(message("1", "Anyone done Q3?"), mine = false, senderName = "bram"), onReply = { replies++ }) }
        }
        composeTestRule.onNodeWithTag(StudyChatTags.message("1")).performTouchInput { swipeRight(startX = centerX - 200f, endX = centerX + 200f) }
        composeTestRule.waitForIdle()
        assertEquals(1, replies)
        // TalkBack's Reply action, as it calls it.
        val node = composeTestRule.onNodeWithTag(StudyChatTags.message("1")).fetchSemanticsNode()
        val reply = node.config[androidx.compose.ui.semantics.SemanticsActions.CustomActions].single { it.label == "Reply" }
        composeTestRule.runOnUiThread { reply.action() }
        assertEquals(2, replies)
    }

    @Test
    fun aDeletedMessage_isntAnswered() {
        var replies = 0
        composeTestRule.setContent {
            PixelQuestTheme { MessageBubbleRow(ChatMessageRow(message("1", null, deleted = true), mine = false, senderName = "bram"), onReply = { replies++ }) }
        }
        composeTestRule.onNodeWithTag(StudyChatTags.message("1")).performTouchInput { swipeRight(startX = centerX - 200f, endX = centerX + 200f) }
        composeTestRule.waitForIdle()
        assertEquals(0, replies)
    }

    @Test
    fun aReply_quotesWhatItAnswers_andThePreviewCanBeCancelled() {
        val asked = message("1", "Anyone done Q3?")
        var cancelled = false
        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column {
                    MessageBubbleRow(ChatMessageRow(message("2", "Yes: 42", sender = "aria"), mine = true, senderName = "aria_k", replyTo = asked, replyToName = "bram"))
                    ReplyPreview(ChatMessageRow(asked, mine = false, senderName = "bram"), onCancel = { cancelled = true })
                }
            }
        }
        assertEquals("in the bubble, and over the input", 2, composeTestRule.onAllNodesWithTag(StudyChatTags.QUOTE, useUnmergedTree = true).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("Replying to").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.REPLY_CANCEL).performClick()
        assertEquals(true, cancelled)
    }
}
