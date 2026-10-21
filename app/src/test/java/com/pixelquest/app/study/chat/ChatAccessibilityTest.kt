package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.MessageBubbleRow
import com.pixelquest.app.ui.chat.bubbleTime
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** TalkBack reads each study chat bubble as one sentence. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatAccessibilityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aBubble_isOneSentence() {
        val at = Instant.parse("2026-10-21T10:05:00Z")
        val message = ChatMessage("m1", "p1", "bram", ChatMessageKind.TEXT, "Anyone done Q3?", null, null, null, at, null)
        composeTestRule.setContent { PixelQuestTheme { MessageBubbleRow(ChatMessageRow(message, mine = false, senderName = "bram")) } }
        composeTestRule.onNodeWithContentDescription("bram said: Anyone done Q3?, at ${bubbleTime(at)}.").assertExists()
    }
}
