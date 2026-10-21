package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.ThemeMode
import com.pixelquest.app.study.chat.data.local.UploadState
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.study.chat.domain.ChatMessage
import com.pixelquest.app.study.chat.domain.ChatMessageKind
import com.pixelquest.app.study.chat.ui.ChatActions
import com.pixelquest.app.study.chat.ui.ChatMessageRow
import com.pixelquest.app.study.chat.ui.ChatUiState
import com.pixelquest.app.study.chat.ui.ChatUploadRow
import com.pixelquest.app.study.chat.ui.StudyChatPane
import com.pixelquest.app.ui.chat.ChatLook
import com.pixelquest.app.ui.chat.LocalChatSimpleMode
import com.pixelquest.app.ui.chat.chatLook
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** The study chat in every look: Comic and Pixel skins, and Simple Mode, as the Darood chat draws them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class StudyChatLooksTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val file = ChatFile("f1", "p1", "bram", "Past paper.pdf", "application/pdf", 1200, null, ChatFileStatus.READY, null, false, Instant.EPOCH, null)
    private val state = ChatUiState(
        loaded = true,
        rows = listOf(
            ChatMessageRow(ChatMessage("m1", "p1", "bram", ChatMessageKind.TEXT, "Anyone done Q3?", null, null, null, Instant.parse("2026-10-21T10:00:00Z"), null), mine = false, senderName = "bram"),
            ChatMessageRow(ChatMessage("m2", "p1", "bram", ChatMessageKind.FILE, null, "f1", null, null, Instant.parse("2026-10-21T10:01:00Z"), null), mine = false, senderName = "bram", file = file),
            ChatMessageRow(ChatMessage("m3", "p1", "aria", ChatMessageKind.TEXT, "Yes: 42", null, null, null, Instant.parse("2026-10-21T10:02:00Z"), null), mine = true, senderName = "aria_k")
        ),
        uploads = listOf(ChatUploadRow("u1", "Notes.docx", "application/msword", 1000, 500, UploadState.UPLOADING, null))
    )

    private fun shows(theme: ThemeMode, simple: Boolean): ChatLook {
        var look: ChatLook? = null
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = theme) {
                CompositionLocalProvider(LocalChatSimpleMode provides simple) {
                    look = chatLook()
                    StudyChatPane(state, ZoneId.of("UTC"), ChatActions(onAttach = {}))
                }
            }
        }
        composeTestRule.onNodeWithText("Anyone done Q3?").assertExists()
        composeTestRule.onNodeWithText("Past paper.pdf").assertExists()
        composeTestRule.onNodeWithText("Notes.docx").assertExists()
        return look!!
    }

    @Test
    fun comic_roundAndShadowed() {
        val look = shows(ThemeMode.Comic, simple = false)
        assertEquals(16.dp, look.corner)
        assertEquals(2.dp, look.shadow)
    }

    @Test
    fun pixel_squareCorners() {
        assertEquals(0.dp, shows(ThemeMode.Pixel, simple = false).corner)
    }

    @Test
    fun simpleMode_keepsTheInk_dropsTheShadows() {
        val look = shows(ThemeMode.Comic, simple = true)
        assertEquals(0.dp, look.shadow)
        assertEquals(true, look.outline > 0.dp)
    }
}
