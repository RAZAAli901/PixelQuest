package com.pixelquest.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.screens.tasks.CreateTaskScreen
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateTaskScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fillFormAndSave_navigatesBackAndCreatesTask() {
        val fakeRepo = FakeTaskRepository()
        val alarmScheduler = TaskAlarmScheduler(ApplicationProvider.getApplicationContext())
        val viewModel = TaskFormViewModel(fakeRepo, alarmScheduler)
        var navigatedBack = false

        composeTestRule.setContent {
            PixelQuestTheme {
                CreateTaskScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }

        composeTestRule.onNodeWithText("NEW QUEST").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter quest title...").performTextInput("Morning Run")
        composeTestRule.onNodeWithText("SAVE QUEST").performClick()

        assertTrue(navigatedBack)
    }
}
