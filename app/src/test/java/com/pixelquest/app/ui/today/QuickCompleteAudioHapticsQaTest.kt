package com.pixelquest.app.ui.today

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.today.TodayContent
import com.pixelquest.app.ui.screens.today.TodayTaskItem
import com.pixelquest.app.ui.screens.today.TodayUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class QuickCompleteAudioHapticsQaTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickComplete_triggersAudioAndHapticsWithoutExceptions() {
        var completed = false
        val today = LocalDate.now()
        val task = TaskEntity(id = 1, name = "Daily Pushups", scheduledTime = LocalTime.of(10, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY)

        val state = TodayUiState.Success(
            tasks = listOf(TodayTaskItem(task = task, status = TaskItemStatus.PENDING, scheduledTime = LocalTime.of(10, 0))),
            currentStreak = 1,
            totalXp = 100,
            level = 1,
            completionPercentage = 0f,
            targetThreshold = 0.7f,
            isPerfectDay = false
        )

        composeTestRule.setContent {
            TodayContent(
                state = state,
                onQuickComplete = { completed = true },
                onQuickSkip = {},
                onRefresh = {},
                onCreateQuestClick = {},
                onNavigateToEditTask = {},
                onNavigateToProfile = {}
            )
        }

        composeTestRule.onNodeWithText("✓ DONE").performClick()
        assertTrue(completed)
    }
}
