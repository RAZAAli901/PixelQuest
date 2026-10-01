package com.pixelquest.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelCategorySelector
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelDaySelector
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelRecurrenceSelector
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTimePicker
import com.pixelquest.app.ui.theme.PixelTheme
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CreateTaskScreen(
    viewModel: TaskFormViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val formState by viewModel.formState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    TaskFormContent(
        formState = formState,
        showDeleteConfirm = showDeleteConfirm,
        onShowDeleteConfirm = { showDeleteConfirm = it },
        onNameChanged = { viewModel.onNameChanged(it) },
        onRecurrenceSelected = { viewModel.onRecurrenceSelected(it) },
        onDayToggled = { viewModel.onDayToggled(it) },
        onTimeSelected = { viewModel.onTimeSelected(it) },
        onCategorySelected = { viewModel.onCategorySelected(it) },
        onReminderEnabledChanged = { viewModel.onReminderEnabledChanged(it) },
        onReminderLeadSelected = { viewModel.onReminderLeadSelected(it) },
        onReminderStyleSelected = { viewModel.onReminderStyleSelected(it) },
        onSave = {
            viewModel.saveTask {
                onNavigateBack()
            }
        },
        onDelete = {
            viewModel.deleteTask {
                onNavigateBack()
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun TaskFormContent(
    formState: TaskFormState,
    showDeleteConfirm: Boolean = false,
    onShowDeleteConfirm: (Boolean) -> Unit = {},
    onNameChanged: (String) -> Unit = {},
    onRecurrenceSelected: (RecurrenceType) -> Unit = {},
    onDayToggled: (java.time.DayOfWeek) -> Unit = {},
    onTimeSelected: (java.time.LocalTime) -> Unit = {},
    onCategorySelected: (com.pixelquest.app.domain.model.TaskCategory) -> Unit = {},
    onReminderEnabledChanged: (Boolean) -> Unit = {},
    onReminderLeadSelected: (Int) -> Unit = {},
    onReminderStyleSelected: (com.pixelquest.app.domain.model.ReminderStyle) -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    if (showDeleteConfirm) {
        PixelConfirmDialog(
            title = "ABANDON QUEST?",
            message = "Are you sure you want to delete '${formState.name}'?",
            confirmText = "DELETE",
            dismissText = "CANCEL",
            onConfirm = {
                onShowDeleteConfirm(false)
                onDelete()
            },
            onDismiss = { onShowDeleteConfirm(false) }
        )
    }

    Scaffold(
        topBar = {
            com.pixelquest.app.ui.components.PixelTopAppBar(
                title = if (formState.isEditMode) "EDIT QUEST" else "NEW QUEST",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Go Back"
                        }
                    ) {
                        Text("◀", style = MaterialTheme.typography.titleMedium, color = PixelTheme.colors.primary)
                    }
                },
                actions = {
                    if (formState.isEditMode) {
                        IconButton(
                            onClick = { onShowDeleteConfirm(true) },
                            modifier = Modifier.semantics {
                                contentDescription = "Delete Quest"
                            }
                        ) {
                            Text("🗑️", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            )
        },
        containerColor = PixelTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PixelTheme.colors.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelTextField(
                value = formState.name,
                onValueChange = onNameChanged,
                label = "QUEST NAME",
                placeholder = "Enter quest title...",
                errorText = formState.nameError,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelRecurrenceSelector(
                selectedType = formState.recurrenceType,
                onTypeSelected = onRecurrenceSelected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (formState.recurrenceType == RecurrenceType.WEEKLY) {
                PixelDaySelector(
                    selectedDays = formState.selectedDays,
                    onDayToggled = onDayToggled,
                    errorText = formState.daysError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            PixelTimePicker(
                selectedTime = formState.scheduledTime,
                onTimeSelected = onTimeSelected,
                errorText = formState.timeError,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            PixelCategorySelector(
                selectedCategory = formState.category,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            ReminderSettingsSection(
                reminderEnabled = formState.reminderEnabled,
                leadMinutes = formState.reminderLeadMinutes,
                style = formState.reminderStyle,
                onReminderEnabledChanged = onReminderEnabledChanged,
                onLeadSelected = onReminderLeadSelected,
                onStyleSelected = onReminderStyleSelected
            )
            Spacer(modifier = Modifier.height(24.dp))

            PixelButton(
                text = if (formState.isEditMode) "UPDATE QUEST" else "SAVE QUEST",
                onClick = onSave,
                variant = PixelButtonVariant.YELLOW,
                enabled = !formState.isSubmitting && formState.isValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Create Task Screen - Comic Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun CreateTaskScreenComicPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme(themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        TaskFormContent(
            formState = TaskFormState(
                name = "Defeat the Dragon",
                isEditMode = false,
                category = com.pixelquest.app.domain.model.TaskCategory.FITNESS,
                recurrenceType = RecurrenceType.DAILY
            )
        )
    }
}

@Preview(name = "Edit Task Screen - Comic Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun EditTaskScreenComicPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme(themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        TaskFormContent(
            formState = TaskFormState(
                name = "Read Comic Chapter 5",
                isEditMode = true,
                category = com.pixelquest.app.domain.model.TaskCategory.LEARNING,
                recurrenceType = RecurrenceType.WEEKLY,
                selectedDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY)
            )
        )
    }
}

