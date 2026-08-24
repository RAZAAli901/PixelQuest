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
class TodayScreenEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun todayScreen_rendersFullUiStateAndRespondsToActions() {
        var completedTask: TaskEntity? = null
        val today = LocalDate.now()
        val task = TaskEntity(
            id = 1,
            name = "Epic Workout",
            scheduledTime = LocalTime.of(15, 0),
            scheduledDay = today,
            category = TaskCategory.FITNESS,
            recurrenceType = RecurrenceType.DAILY
        )

        val state = TodayUiState.Success(
            tasks = listOf(TodayTaskItem(task = task, status = TaskItemStatus.PENDING, scheduledTime = LocalTime.of(15, 0))),
            currentStreak = 5,
            totalXp = 800,
            level = 3,
            completionPercentage = 0.5f,
            targetThreshold = 0.7f,
            isPerfectDay = false,
            flavorText = "Halfway there, hero!"
        )

        composeTestRule.setContent {
            TodayContent(
                state = state,
                onQuickComplete = { completedTask = it },
                onQuickSkip = {},
                onRefresh = {},
                onCreateQuestClick = {},
                onNavigateToEditTask = {},
                onNavigateToProfile = {}
            )
        }

        composeTestRule.onNodeWithText("⚔️ TODAY'S DASHBOARD").assertExists()
        composeTestRule.onNodeWithText("5 DAYS").assertExists()
        composeTestRule.onNodeWithText("800 XP").assertExists()
        composeTestRule.onNodeWithText("LVL 3").assertExists()
        composeTestRule.onNodeWithText("Epic Workout").assertExists()
        composeTestRule.onNodeWithText("💬 Halfway there, hero!").assertExists()

        composeTestRule.onNodeWithText("✓ DONE").performClick()
        assertTrue(completedTask?.id == 1L)
    }
}
