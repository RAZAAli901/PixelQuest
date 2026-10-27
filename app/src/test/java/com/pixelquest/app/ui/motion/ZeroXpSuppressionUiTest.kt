package com.pixelquest.app.ui.motion

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestRow
import com.pixelquest.app.ui.components.QuestUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * B08: Verifies that when 0 XP is awarded, no XP popup or +XP animation is shown.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ZeroXpSuppressionUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun questRow_whenCompletedWithZeroXp_showsNoFloatingXpGain() {
        var quest by mutableStateOf(
            QuestUi(
                id = 1L,
                title = "Reading Quest",
                time = "10:00",
                category = QuestCategory.Fitness,
                done = false,
                xp = 0,
            ),
        )

        composeTestRule.setContent {
            QuestRow(
                quest = quest,
                onToggle = {},
                onClick = {},
            )
        }
        composeTestRule.waitForIdle()

        // Toggle to completed with 0 XP
        quest = quest.copy(done = true, xp = 0)
        composeTestRule.waitForIdle()

        // Assert no node with "+0 XP" or any XP gain text exists
        composeTestRule.onNode(hasText("+0 XP", substring = true)).assertDoesNotExist()
        composeTestRule.onNode(hasText("XP", substring = true)).assertDoesNotExist()
    }

    @Test
    fun xpGainChip_withZeroAmount_doesNotRender() {
        composeTestRule.setContent {
            XpGainChip(amount = 0, trigger = 1)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("XP", substring = true)).assertDoesNotExist()
    }

    @Test
    fun floatingReward_withZeroText_doesNotRender() {
        composeTestRule.setContent {
            FloatingReward(text = "+0 XP", trigger = 1)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("+0 XP").assertDoesNotExist()
    }
}
