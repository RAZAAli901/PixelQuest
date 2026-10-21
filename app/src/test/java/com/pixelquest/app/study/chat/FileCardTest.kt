package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.FileCard
import com.pixelquest.app.study.chat.ui.MessageBubbleRow
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** A shared file in the chat: its kind, name, size and sharer, and whether it opens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class FileCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun file(status: ChatFileStatus, reason: String? = null) =
        ChatFile("f1", "p1", "bram", "Past paper.pdf", "application/pdf", 1_258_291, "ab", status, reason, false, Instant.EPOCH, null)

    @Test
    fun aReadyFile_saysWhatItIs_andOpens() {
        var opened: String? = null
        composeTestRule.setContent { PixelQuestTheme { FileCard(file(ChatFileStatus.READY), onOpen = { opened = it.id }, uploader = "bram") } }
        composeTestRule.onNodeWithText("Past paper.pdf").assertExists()
        composeTestRule.onNodeWithText("PDF · 1.3 MB · from bram").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.file("f1")).assertHasClickAction().performClick()
        assertEquals("f1", opened)
    }

    @Test
    fun aRejectedOrCheckingFile_saysSo_andDoesntOpen() {
        composeTestRule.setContent { PixelQuestTheme { FileCard(file(ChatFileStatus.REJECTED, "macros"), onOpen = {}) } }
        composeTestRule.onNodeWithText("Not shared: it has macros").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.file("f1")).assertHasNoClickAction()
    }

    @Test
    fun aFileMessage_showsItsFileInItsBubble() {
        val message = ChatMessage("m1", "p1", "bram", ChatMessageKind.FILE, null, "f1", null, null, Instant.EPOCH, null)
        composeTestRule.setContent {
            PixelQuestTheme { MessageBubbleRow(ChatMessageRow(message, mine = false, senderName = "bram", file = file(ChatFileStatus.READY)), onOpenFile = {}) }
        }
        composeTestRule.onNodeWithText("Past paper.pdf").assertExists()
        composeTestRule.onNodeWithText("@bram").assertExists()
    }
}
