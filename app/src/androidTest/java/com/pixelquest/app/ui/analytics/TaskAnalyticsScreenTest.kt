package com.pixelquest.app.ui.analytics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.screens.analytics.TaskAnalyticsContent
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskAnalyticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun analyticsScreen_rendersTaskMetricsCorrectly() {
        val sampleTask = TaskEntity(
            id = 1L,
            name = "Read Books",
            description = "Daily reading",
            scheduledDay = LocalDate.now(),
            scheduledTime = LocalTime.of(20, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.LEARNING
        )

        val sampleStats = PerTaskStats(
            taskId = 1L,
            completionCount = 15,
            totalScheduledCount = 20,
            completionRate = 0.75f,
            currentStreak = 5,
            longestStreak = 8
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                TaskAnalyticsContent(
                    task = sampleTask,
                    stats = sampleStats,
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("READ BOOKS").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 DAYS").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 DAYS").assertIsDisplayed()
    }
}
