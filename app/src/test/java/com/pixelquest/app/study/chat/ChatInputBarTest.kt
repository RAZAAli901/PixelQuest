package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.pixelquest.app.study.chat.ui.ChatInputBar
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The chat's input: text up to 2,000 characters, a counter near them, and a send that's only there for something to send. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ChatInputBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sending_isForSomethingToSend() {
        var text by mutableStateOf("")
        var sent = 0
        composeTestRule.setContent { PixelQuestTheme { ChatInputBar(value = text, onValueChange = { text = it }, onSend = { sent++ }) } }
        composeTestRule.onNodeWithTag(StudyChatTags.SEND).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(StudyChatTags.INPUT).performTextInput("   ")
        composeTestRule.onNodeWithTag(StudyChatTags.SEND).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(StudyChatTags.INPUT).performTextReplacement("Anyone done Q3?")
        composeTestRule.onNodeWithTag(StudyChatTags.SEND).assertIsEnabled().performClick()
        assertEquals(1, sent)
    }

    @Test
    fun theCounter_comesNearTheLimit_andTheTextStopsAtIt() {
        var text by mutableStateOf("")
        composeTestRule.setContent { PixelQuestTheme { ChatInputBar(value = text, onValueChange = { text = it }, onSend = {}) } }
        composeTestRule.onNodeWithTag(StudyChatTags.COUNTER).assertDoesNotExist()
        text = "x".repeat(1799)
        composeTestRule.onNodeWithTag(StudyChatTags.COUNTER).assertDoesNotExist()
        text = "x".repeat(1800)
        composeTestRule.onNodeWithTag(StudyChatTags.COUNTER).assertTextEquals("1,800 of 2,000")
        composeTestRule.onNodeWithTag(StudyChatTags.INPUT).performTextReplacement("y".repeat(2500))
        assertEquals(2000, text.length)
        composeTestRule.onNodeWithTag(StudyChatTags.COUNTER).assertTextEquals("2,000 of 2,000")
    }
}
