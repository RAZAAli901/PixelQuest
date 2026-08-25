package com.pixelquest.app.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.screens.history.EmptyHistoryState
import com.pixelquest.app.ui.screens.history.TaskHistoryItem
import com.pixelquest.app.ui.screens.history.TaskHistoryListItem
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TaskHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_rendersCorrectly() {
        composeTestRule.setContent {
            PixelQuestTheme {
                EmptyHistoryState()
            }
        }

        composeTestRule.onNodeWithText("NO QUEST HISTORY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Complete your daily quests to build your log history!").assertIsDisplayed()
    }

    @Test
    fun populatedItem_rendersTaskHistoryDetails() {
        val sampleItem = TaskHistoryItem(
            logId = 1L,
            taskId = 10L,
            taskName = "Read 20 Pages",
            category = TaskCategory.LEARNING,
            completedDate = LocalDate.of(2026, 8, 20),
            wasCompleted = true,
            pointsAwarded = 25
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                TaskHistoryListItem(
                    item = sampleItem,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Read 20 Pages").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETED").assertIsDisplayed()
        composeTestRule.onNodeWithText("+25 XP").assertIsDisplayed()
    }
}
