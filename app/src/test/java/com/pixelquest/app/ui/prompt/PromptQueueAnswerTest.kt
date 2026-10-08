package com.pixelquest.app.ui.prompt

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Answering one queued prompt shows the next one, and only the next one. On Day 30 the queue moved on
 * twice per answer (on dismiss, and again when the answer was saved), so the second of two prompts
 * due in the same minute appeared and was then skipped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class PromptQueueAnswerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eachAnswer_movesOnExactlyOnce() {
        var queue by mutableStateOf(PromptQueue(listOf(PromptRequest(1, "Run"), PromptRequest(2, "Read"))))
        val answers = mutableListOf<Pair<Long, Boolean>>()
        // Like the ViewModel: the save finishes after the screen has already moved on.
        val pendingSaves = mutableListOf<() -> Unit>()

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                queue.current?.let { prompt ->
                    PromptQueueContent(
                        prompt = prompt,
                        isSimpleMode = false,
                        onAnswer = { id, done -> pendingSaves += { answers += id to done } },
                        onNext = { queue = queue.advance() }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("YES!").performClick()
        pendingSaves.forEach { it() }
        composeTestRule.waitForIdle()
        assertEquals("Read is still waiting after Run is answered", PromptRequest(2, "Read"), queue.current)
        composeTestRule.onNodeWithText("Read").assertExists()

        composeTestRule.onNodeWithText("NOT YET").performClick()
        composeTestRule.waitForIdle()
        assertNull(queue.current)
        pendingSaves.drop(1).forEach { it() }
        assertEquals(listOf(1L to true, 2L to false), answers)
    }
}
