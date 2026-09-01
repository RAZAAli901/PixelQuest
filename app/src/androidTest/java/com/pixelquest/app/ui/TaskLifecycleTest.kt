package com.pixelquest.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskLifecycleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun taskLifecycle_createEditDelete_updatesTaskListCorrectly() {
        val tasks = mutableStateListOf<TaskEntity>()
        var screenState by mutableStateOf("LIST") // LIST, CREATE, EDIT
        var selectedTaskForEdit by mutableStateOf<TaskEntity?>(null)
        var showDeleteDialog by mutableStateOf(false)

        composeTestRule.setContent {
            PixelQuestTheme {
                when (screenState) {
                    "LIST" -> {
                        androidx.compose.foundation.layout.Column {
                            PixelButton(text = "NEW QUEST", onClick = { screenState = "CREATE" })
                            tasks.forEach { task ->
                                androidx.compose.foundation.layout.Row {
                                    androidx.compose.material3.Text(
                                        text = task.name,
                                        modifier = androidx.compose.ui.Modifier.clickable {
                                            selectedTaskForEdit = task
                                            screenState = "EDIT"
                                        }
                                    )
                                    PixelButton(text = "DELETE", onClick = {
                                        selectedTaskForEdit = task
                                        showDeleteDialog = true
                                    })
                                }
                            }
                        }

                        if (showDeleteDialog && selectedTaskForEdit != null) {
                            PixelConfirmDialog(
                                title = "DELETE QUEST",
                                message = "Are you sure you want to delete '${selectedTaskForEdit?.name}'?",
                                onConfirm = {
                                    tasks.removeIf { it.id == selectedTaskForEdit?.id }
                                    showDeleteDialog = false
                                    selectedTaskForEdit = null
                                },
                                onDismiss = { showDeleteDialog = false }
                            )
                        }
                    }
                    "CREATE" -> {
                        var title by remember { mutableStateOf("") }
                        androidx.compose.foundation.layout.Column {
                            PixelTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = "Quest Title",
                                placeholder = "Enter quest title..."
                            )
                            PixelButton(text = "SAVE QUEST", onClick = {
                                val newTask = TaskEntity(
                                    id = tasks.size.toLong() + 1,
                                    name = title,
                                    description = "",
                                    scheduledDay = LocalDate.now(),
                                    scheduledTime = LocalTime.of(10, 0),
                                    recurrenceType = RecurrenceType.DAILY,
                                    category = TaskCategory.HEALTH
                                )
                                tasks.add(newTask)
                                screenState = "LIST"
                            })
                        }
                    }
                    "EDIT" -> {
                        var title by remember { mutableStateOf(selectedTaskForEdit?.name ?: "") }
                        androidx.compose.foundation.layout.Column {
                            PixelTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = "Quest Title",
                                placeholder = "Edit quest title..."
                            )
                            PixelButton(text = "UPDATE QUEST", onClick = {
                                val index = tasks.indexOfFirst { it.id == selectedTaskForEdit?.id }
                                if (index != -1) {
                                    tasks[index] = tasks[index].copy(name = title)
                                }
                                screenState = "LIST"
                            })
                        }
                    }
                }
            }
        }

        // 1. Create Task
        composeTestRule.onNodeWithText("NEW QUEST").performClick()
        composeTestRule.onNodeWithText("Enter quest title...").performTextInput("Meditation")
        composeTestRule.onNodeWithText("SAVE QUEST").performClick()

        // Verify created
        composeTestRule.onNodeWithText("Meditation").assertIsDisplayed()
        assertEquals(1, tasks.size)

        // 2. Edit Task
        composeTestRule.onNodeWithText("Meditation").performClick()
        composeTestRule.onNodeWithText("Meditation").performTextClearance()
        composeTestRule.onNodeWithText("Edit quest title...").performTextInput("Deep Meditation")
        composeTestRule.onNodeWithText("UPDATE QUEST").performClick()

        // Verify edited
        composeTestRule.onNodeWithText("Deep Meditation").assertIsDisplayed()
        assertEquals("Deep Meditation", tasks[0].name)

        // 3. Delete Task
        composeTestRule.onNodeWithText("DELETE").performClick()
        composeTestRule.onNodeWithText("CONFIRM").performClick()

        // Verify deleted
        assertEquals(0, tasks.size)
    }
}
