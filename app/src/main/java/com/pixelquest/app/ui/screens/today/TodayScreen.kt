package com.pixelquest.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelDailyProgressRing
import com.pixelquest.app.ui.components.PixelErrorState
import com.pixelquest.app.ui.components.PixelLoadingState
import com.pixelquest.app.ui.components.PixelPerfectDayBanner
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.components.TodayQuestCard
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToEditTask: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val soundManager = LocalSoundManager.current

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
                    onQuickComplete = { task ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        soundManager?.playTaskCompleteSound()
                        viewModel.completeTask(task)
                    },
                    onQuickSkip = { task ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        soundManager?.playTaskMissedSound()
                        viewModel.skipTask(task)
                    },
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
    onQuickComplete: (TaskEntity) -> Unit,
    onQuickSkip: (TaskEntity) -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var taskToSkip by remember { mutableStateOf<TaskEntity?>(null) }

    val pendingTasks = state.tasks.filter { it.status == TaskItemStatus.PENDING }
    val completedOrMissedTasks = state.tasks.filter { it.status != TaskItemStatus.PENDING }

    if (taskToSkip != null) {
        PixelConfirmDialog(
            title = "SKIP QUEST",
            message = "Are you sure you want to mark '${taskToSkip?.name}' as missed/skipped?",
            onConfirm = {
                taskToSkip?.let { onQuickSkip(it) }
                taskToSkip = null
            },
            onDismiss = { taskToSkip = null }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PixelDailyProgressRing(
                progress = state.completionPercentage,
                targetThreshold = state.targetThreshold
            )
        }
        if (state.isPerfectDay) {
            item {
                PixelPerfectDayBanner()
            }
        }
        if (pendingTasks.isNotEmpty()) {
            item {
                Text(
                    text = "⚔️ UP NEXT",
                    style = PixelTypography.titleMedium,
                    color = PixelGold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(
                items = pendingTasks,
                key = { it.task.id }
            ) { item ->
                TodayQuestCard(
                    task = item.task,
                    status = item.status,
                    onQuickComplete = { onQuickComplete(item.task) },
                    onQuickSkip = { taskToSkip = item.task },
                    onClick = { onNavigateToEditTask(item.task.id) }
                )
            }
        }

        if (completedOrMissedTasks.isNotEmpty()) {
            item {
                Text(
                    text = "📜 COMPLETED & PAST QUESTS",
                    style = PixelTypography.titleMedium,
                    color = PixelTextMuted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(
                items = completedOrMissedTasks,
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
}
