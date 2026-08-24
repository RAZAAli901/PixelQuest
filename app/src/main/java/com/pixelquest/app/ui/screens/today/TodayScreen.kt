package com.pixelquest.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.ui.components.PixelErrorState
import com.pixelquest.app.ui.components.PixelLoadingState
import com.pixelquest.app.ui.components.TodayQuestCard
import com.pixelquest.app.ui.theme.PixelBackgroundDark

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToEditTask: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
    ) {
        when (val state = uiState) {
            is TodayUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PixelLoadingState()
                }
            }
            is TodayUiState.Error -> {
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
            is TodayUiState.Success -> {
                TodayContent(
                    state = state,
                    onNavigateToEditTask = onNavigateToEditTask,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
fun TodayContent(
    state: TodayUiState.Success,
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = state.tasks,
            key = { it.task.id }
        ) { item ->
            TodayQuestCard(
                task = item.task,
                status = item.status,
                onQuickComplete = {},
                onQuickSkip = {},
                onClick = { onNavigateToEditTask(item.task.id) }
            )
        }
    }
}
