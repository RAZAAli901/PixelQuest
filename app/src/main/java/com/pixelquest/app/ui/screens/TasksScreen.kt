package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.pixelquest.app.ui.components.PixelErrorState
import com.pixelquest.app.ui.components.PixelLoadingState
import com.pixelquest.app.ui.components.PixelTaskListItem
import com.pixelquest.app.ui.screens.tasks.TaskUiState
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TasksScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onNavigateToCreateTask: () -> Unit = {},
    onNavigateToEditTask: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateTask,
                containerColor = PixelGold,
                contentColor = PixelBackgroundDark,
                shape = CutCornerShape(4.dp)
            ) {
                Text(
                    text = "+",
                    style = PixelTypography.displaySmall,
                    color = PixelBackgroundDark
                )
            }
        },
        containerColor = PixelBackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PixelBackgroundDark)
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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
