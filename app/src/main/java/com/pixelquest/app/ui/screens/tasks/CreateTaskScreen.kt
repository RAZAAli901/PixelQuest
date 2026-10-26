package com.pixelquest.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.ui.components.PixelCategorySelector
import com.pixelquest.app.ui.components.PixelDaySelector
import com.pixelquest.app.ui.components.PixelRecurrenceSelector
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTimePicker
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqIconButton
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType

@Composable
fun CreateTaskScreen(
    viewModel: TaskFormViewModel,
    onNavigateBack: () -> Unit = {},
) {
    val formState by viewModel.formState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val askForNotifications = com.pixelquest.app.ui.notifications.rememberAskForNotifications()

    TaskFormContent(
        formState = formState,
        showDeleteConfirm = showDeleteConfirm,
        onShowDeleteConfirm = { showDeleteConfirm = it },
        onNameChanged = { viewModel.onNameChanged(it) },
        onRecurrenceSelected = { viewModel.onRecurrenceSelected(it) },
        onDayToggled = { viewModel.onDayToggled(it) },
        onTimeSelected = { viewModel.onTimeSelected(it) },
        onCategorySelected = { viewModel.onCategorySelected(it) },
        onReminderEnabledChanged = {
            if (it) askForNotifications()
            viewModel.onReminderEnabledChanged(it)
        },
        onReminderLeadSelected = { viewModel.onReminderLeadSelected(it) },
        onReminderStyleSelected = { viewModel.onReminderStyleSelected(it) },
        onSave = {
            // A new quest's reminder is on from the start: saving it is turning it on.
            if (formState.reminderEnabled) askForNotifications()
            viewModel.saveTask {
                onNavigateBack()
            }
        },
        onDelete = {
            viewModel.deleteTask {
                onNavigateBack()
            }
        },
        onNavigateBack = onNavigateBack,
        canScheduleExact = { viewModel.canScheduleExactAlarms() },
        onAllowExact = { context.startActivity(viewModel.exactAlarmSettingsIntent()) },
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
    onNavigateBack: () -> Unit = {},
    canScheduleExact: () -> Boolean = { true },
    onAllowExact: () -> Unit = {},
) {
    if (showDeleteConfirm) {
        PqConfirmDialog(
            title = "Abandon quest?",
            message = "Are you sure you want to delete '${formState.name}'?",
            confirm = "Delete",
            dismiss = "Cancel",
            onConfirm = {
                onShowDeleteConfirm(false)
                onDelete()
            },
            onDismiss = { onShowDeleteConfirm(false) },
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 32.dp),
    ) {
        PqTopBar(if (formState.isEditMode) "Edit quest" else "New quest", onBack = onNavigateBack) {
            if (formState.isEditMode) PqIconButton(Icons.Rounded.Delete, "Delete quest", { onShowDeleteConfirm(true) })
        }
        PixelTextField(
            value = formState.name,
            onValueChange = onNameChanged,
            label = "Quest name",
            placeholder = "Enter quest title...",
            errorText = formState.nameError,
            modifier = Modifier.fillMaxWidth(),
        )
        PixelRecurrenceSelector(
            selectedType = formState.recurrenceType,
            onTypeSelected = onRecurrenceSelected,
            modifier = Modifier.fillMaxWidth(),
        )
        if (formState.recurrenceType == RecurrenceType.WEEKLY) {
            PixelDaySelector(
                selectedDays = formState.selectedDays,
                onDayToggled = onDayToggled,
                errorText = formState.daysError,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PixelTimePicker(
                selectedTime = formState.scheduledTime,
                onTimeSelected = onTimeSelected,
                errorText = formState.timeError,
                modifier = Modifier.fillMaxWidth(),
            )
            if (formState.startsTomorrow) {
                Text(text = stringResource(R.string.pq_quest_starts_tomorrow), style = PqType.Meta)
            }
        }
        PixelCategorySelector(
            selectedCategory = formState.category,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.fillMaxWidth(),
        )
        ReminderSettingsSection(
            reminderEnabled = formState.reminderEnabled,
            leadMinutes = formState.reminderLeadMinutes,
            style = formState.reminderStyle,
            onReminderEnabledChanged = onReminderEnabledChanged,
            onLeadSelected = onReminderLeadSelected,
            onStyleSelected = onReminderStyleSelected,
        )
        // Where the reminder is set: without "Alarms & reminders" Android may ring it up to 10
        // minutes late (seen on the emulator), and the note was only in Settings.
        if (formState.reminderEnabled) {
            com.pixelquest.app.ui.screens.settings.ExactAlarmPrompt(canScheduleExact = canScheduleExact, onAllow = onAllowExact)
        }
        PqButton(
            text = if (formState.isEditMode) "Update quest" else "Save quest",
            onClick = onSave,
            enabled = !formState.isSubmitting && formState.isValid,
            height = 52.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        if (formState.isEditMode) {
            PqButton(
                text = "Delete quest",
                onClick = { onShowDeleteConfirm(true) },
                style = PqButtonStyle.Secondary,
                height = 52.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
