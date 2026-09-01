package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.components.TodayQuestCard
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskQuickCompleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun taskQuickComplete_updatesStateAndDisplaysDone() {
        val initialTask = TaskEntity(
            id = 1,
            name = "Read 10 Pages",
            description = "Daily reading goal",
            scheduledDay = LocalDate.now(),
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.KNOWLEDGE
        )

        var currentStatus by mutableStateOf(TaskItemStatus.PENDING)
        var totalXp by mutableStateOf(0)
        var currentStreak by mutableStateOf(0)

        composeTestRule.setContent {
            PixelQuestTheme {
                TodayQuestCard(
                    task = initialTask,
                    status = currentStatus,
                    onQuickComplete = {
                        currentStatus = TaskItemStatus.DONE
                        totalXp += 10
                        currentStreak += 1
                    },
                    onQuickSkip = {},
                    onClick = {}
                )
            }
        }

        // Verify task appears on screen
        composeTestRule.onNodeWithText("Read 10 Pages").assertIsDisplayed()

        // Perform quick-complete tap
        composeTestRule.onNodeWithText("✓ DONE").performClick()

        // Verify updated status & state
        assertEquals(TaskItemStatus.DONE, currentStatus)
        assertEquals(10, totalXp)
        assertEquals(1, currentStreak)
    }
}
