package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.EmptyTasksState
import com.pixelquest.app.ui.components.PixelDailyProgressRing
import com.pixelquest.app.ui.components.PixelErrorState
import com.pixelquest.app.ui.components.PixelLoadingState
import com.pixelquest.app.ui.components.PixelPerfectDayBanner
import com.pixelquest.app.ui.components.PixelSnackbar
import com.pixelquest.app.ui.components.PixelTaskListItem
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.screens.tasks.TaskUiState
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TasksScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    TasksContent(
        uiState = uiState,
        onNavigateToCreateTask = onNavigateToCreateTask,
        onNavigateToEditTask = onNavigateToEditTask
    )
}

@Composable
fun TasksContent(
    uiState: TaskUiState,
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (Long) -> Unit = {}
) {
    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors

    Scaffold(
        floatingActionButton = {
            com.pixelquest.app.ui.components.PixelFloatingActionButton(
                onClick = onNavigateToCreateTask
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            when (val state = uiState) {
                is TaskUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelLoadingState()
                    }
                }
                is TaskUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelErrorState(
                            errorMessage = state.message,
                            onRetry = {}
                        )
                    }
                }
                is TaskUiState.Success -> {
                    val missedCount = state.tasks.count { it.status == TaskItemStatus.MISSED }
                    if (state.tasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyTasksState(
                                onCreateQuestClick = onNavigateToCreateTask
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (missedCount > 0) {
                                PixelSnackbar(
                                    message = "$missedCount quest(s) missed today!",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = state.tasks,
                                    key = { it.task.id }
                                ) { item ->
                                    PixelTaskListItem(
                                        task = item.task,
                                        status = item.status,
                                        onClick = { onNavigateToEditTask(item.task.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Tasks Screen - Comic Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun TasksScreenComicPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme(themeMode = com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        val sampleTasks = listOf(
            com.pixelquest.app.ui.screens.tasks.TaskWithStatus(
                task = com.pixelquest.app.data.local.entity.TaskEntity(
                    id = 1,
                    name = "Comic Hero Workout",
                    description = "Power routine",
                    scheduledTime = java.time.LocalTime.of(8, 30),
                    scheduledDay = java.time.LocalDate.now(),
                    category = com.pixelquest.app.domain.model.TaskCategory.FITNESS,
                    recurrenceType = com.pixelquest.app.domain.model.RecurrenceType.DAILY
                ),
                status = TaskItemStatus.PENDING
            ),
            com.pixelquest.app.ui.screens.tasks.TaskWithStatus(
                task = com.pixelquest.app.data.local.entity.TaskEntity(
                    id = 2,
                    name = "Read Comic Issue #42",
                    description = "Graphic novel study",
                    scheduledTime = java.time.LocalTime.of(12, 0),
                    scheduledDay = java.time.LocalDate.now(),
                    category = com.pixelquest.app.domain.model.TaskCategory.LEARNING,
                    recurrenceType = com.pixelquest.app.domain.model.RecurrenceType.DAILY
                ),
                status = TaskItemStatus.COMPLETED
            )
        )
        TasksContent(
            uiState = TaskUiState.Success(tasks = sampleTasks)
        )
    }
}

