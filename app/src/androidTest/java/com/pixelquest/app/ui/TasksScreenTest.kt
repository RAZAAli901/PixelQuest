package com.pixelquest.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.screens.TasksScreen
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TasksScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_rendersCorrectly() {
        val fakeRepo = FakeTaskRepository()
        val viewModel = TaskViewModel(fakeRepo)

        composeTestRule.setContent {
            PixelQuestTheme {
                TasksScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("NO QUESTS YET").assertIsDisplayed()
        composeTestRule.onNodeWithText("CREATE FIRST QUEST").assertIsDisplayed()
    }

    @Test
    fun populatedList_rendersTaskItems() {
        val fakeRepo = FakeTaskRepository()
        runBlocking {
            fakeRepo.insertTask(
                TaskEntity(
                    id = 1,
                    name = "Morning Pushups",
                    description = "",
                    scheduledDay = LocalDate.now(),
                    scheduledTime = LocalTime.of(8, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.FITNESS
                )
            )
        }
        val viewModel = TaskViewModel(fakeRepo)

        composeTestRule.setContent {
            PixelQuestTheme {
                TasksScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Morning Pushups").assertIsDisplayed()
    }
}
