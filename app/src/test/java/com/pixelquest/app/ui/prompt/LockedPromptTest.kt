package com.pixelquest.app.ui.prompt

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S041: over a locked phone the prompt says a quest is waiting, never which one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class LockedPromptTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val prompt = PromptRequest(7, "Therapy session at 5")

    @Test
    fun locked_hidesTheQuest_andOffersToUnlock() {
        var unlocks = 0
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PromptQueueContent(prompt, isSimpleMode = false, onAnswer = { _, _ -> }, onNext = {}, locked = true, onUnlock = { unlocks++ })
            }
        }
        composeTestRule.onAllNodesWithText("Therapy session at 5", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithTag(LockedPromptTags.SCREEN).assertExists()
        composeTestRule.onNodeWithTag(LockedPromptTags.UNLOCK).performClick()
        assertEquals(1, unlocks)
    }

    @Test
    fun unlocked_showsTheQuest() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Comic) {
                PromptQueueContent(prompt, isSimpleMode = false, onAnswer = { _, _ -> }, onNext = {}, locked = false)
            }
        }
        composeTestRule.onAllNodesWithText("Therapy session at 5", substring = true, ignoreCase = true).fetchSemanticsNodes().let { assert(it.isNotEmpty()) }
        composeTestRule.onNodeWithTag(LockedPromptTags.SCREEN).assertDoesNotExist()
    }
}
