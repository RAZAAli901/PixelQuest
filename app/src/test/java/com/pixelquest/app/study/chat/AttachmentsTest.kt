package com.pixelquest.app.study.chat

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.study.chat.data.PickedFile
import com.pixelquest.app.study.chat.ui.ChatActions
import com.pixelquest.app.study.chat.ui.ChatUiState
import com.pixelquest.app.study.chat.ui.StudyChatPane
import com.pixelquest.app.study.chat.ui.StudyChatTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.ZoneId

/** Sharing a file starts with the system's picker, and the file picked waits over the field. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class AttachmentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pdf = PickedFile("content://docs/1", "Past paper.pdf", "application/pdf", 1_258_291)

    @Test
    fun aPickedFile_waitsOverTheField_andCanBeTakenAway_andSent() {
        var removed = false
        composeTestRule.setContent {
            PixelQuestTheme {
                StudyChatPane(
                    state = ChatUiState(loaded = true, attachment = pdf),
                    zone = ZoneId.of("UTC"),
                    actions = ChatActions(onAttach = {}, onRemoveAttachment = { removed = true })
                )
            }
        }
        composeTestRule.onNodeWithTag(StudyChatTags.ATTACH).assertExists()
        composeTestRule.onNodeWithText("Past paper.pdf").assertExists()
        composeTestRule.onNodeWithText("1.3 MB").assertExists()
        composeTestRule.onNodeWithTag(StudyChatTags.SEND).assertIsEnabled()
        composeTestRule.onNodeWithTag(StudyChatTags.ATTACHMENT_REMOVE).performClick()
        assertTrue(removed)
    }

    @Test
    fun withoutAWayToShare_thereIsNoAttachButton() {
        composeTestRule.setContent { PixelQuestTheme { StudyChatPane(ChatUiState(loaded = true), ZoneId.of("UTC"), ChatActions()) } }
        composeTestRule.onNodeWithTag(StudyChatTags.ATTACH).assertDoesNotExist()
    }

    @Test
    fun theAppNeverAsksToReadStorage() {
        val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml")).first { it.exists() }.readText()
        for (permission in listOf("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES")) {
            assertTrue(permission, !manifest.contains(permission))
        }
        assertEquals(true, manifest.contains("android.permission.INTERNET"))
    }
}
