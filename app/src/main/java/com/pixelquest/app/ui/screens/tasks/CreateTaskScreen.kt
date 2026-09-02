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
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun CreateTaskScreen(
    viewModel: TaskFormViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val formState by viewModel.formState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        PixelConfirmDialog(
            title = "ABANDON QUEST?",
            message = "Are you sure you want to delete '${formState.name}'?",
            confirmText = "DELETE",
            dismissText = "CANCEL",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteTask {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        topBar = {
                PixelCard(
                    variant = PixelPanelVariant.BEIGE,
                    contentPadding = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.semantics {
                                contentDescription = "Go Back"
                            }
                        ) {
                            Text("◀", style = PixelTypography.titleMedium, color = PixelGold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (formState.isEditMode) "EDIT QUEST" else "NEW QUEST",
                            style = PixelTypography.titleLarge,
                            color = PixelGold,
                            modifier = Modifier.weight(1f)
                        )
                        if (formState.isEditMode) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.semantics {
                                    contentDescription = "Delete Quest"
                                }
                            ) {
                                Text("🗑️", style = PixelTypography.titleMedium)
                            }
                        }
                    }
                }
            },
            containerColor = PixelBackgroundDark
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PixelBackgroundDark)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelTextField(
                    value = formState.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = "QUEST NAME",
                    placeholder = "Enter quest title...",
                    errorText = formState.nameError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                PixelRecurrenceSelector(
                    selectedType = formState.recurrenceType,
                    onTypeSelected = { viewModel.onRecurrenceSelected(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (formState.recurrenceType == RecurrenceType.WEEKLY) {
                    PixelDaySelector(
                        selectedDays = formState.selectedDays,
                        onDayToggled = { viewModel.onDayToggled(it) },
                        errorText = formState.daysError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                PixelTimePicker(
                    selectedTime = formState.scheduledTime,
                    onTimeSelected = { viewModel.onTimeSelected(it) },
                    errorText = formState.timeError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                PixelCategorySelector(
                    selectedCategory = formState.category,
                    onCategorySelected = { viewModel.onCategorySelected(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                PixelButton(
                    text = if (formState.isEditMode) "UPDATE QUEST" else "SAVE QUEST",
                    onClick = {
                        viewModel.saveTask {
                            onNavigateBack()
                        }
                    },
                    variant = PixelButtonVariant.YELLOW,
                    enabled = !formState.isSubmitting && formState.isValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
