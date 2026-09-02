package com.pixelquest.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.pixelquest.app.ui.components.EmptyTodayState
import com.pixelquest.app.ui.components.FlavorTextBanner
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelDailyProgressRing
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelErrorState
import com.pixelquest.app.ui.components.PixelLoadingState
import com.pixelquest.app.ui.components.PixelPerfectDayBanner
import com.pixelquest.app.ui.components.StreakXpSummaryStrip
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.components.TodayQuestCard
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onNavigateToCreateTask: () -> Unit = {},
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
                        com.pixelquest.app.ui.haptics.PixelHaptics.performSuccessPattern(haptics)
                        soundManager?.playTaskCompleteSound()
                        viewModel.completeTask(task)
                    },
                    onQuickSkip = { task ->
                        com.pixelquest.app.ui.haptics.PixelHaptics.performWarning(haptics)
                        soundManager?.playTaskMissedSound()
                        viewModel.skipTask(task)
                    },
                    onRefresh = { viewModel.refresh() },
                    onCreateQuestClick = onNavigateToCreateTask,
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
    onRefresh: () -> Unit,
    onCreateQuestClick: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var taskToSkip by remember { mutableStateOf<TaskEntity?>(null) }

    val pendingTasks = remember(state.tasks) {
        state.tasks.filter { it.status == TaskItemStatus.PENDING || it.status == TaskItemStatus.GRACE_PERIOD }
    }
    val completedOrMissedTasks = remember(state.tasks) {
        state.tasks.filter { it.status == TaskItemStatus.DONE || it.status == TaskItemStatus.MISSED }
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚔️ TODAY'S DASHBOARD",
                    style = PixelTypography.titleLarge,
                    color = PixelGold
                )
                PixelButton(
                    text = "🔄 REFRESH",
                    onClick = onRefresh,
                    variant = PixelButtonVariant.BLUE
                )
            }
        }
        item {
            PixelDailyProgressRing(
                progress = state.completionPercentage,
                targetThreshold = state.targetThreshold
            )
        }
        item {
            StreakXpSummaryStrip(
                currentStreak = state.currentStreak,
                totalXp = state.totalXp,
                level = state.level,
                onClick = onNavigateToProfile
            )
        }
        if (state.flavorText.isNotBlank()) {
            item {
                FlavorTextBanner(text = state.flavorText)
            }
        }
        if (state.isStreakBroken) {
            item {
                val soundManager = com.pixelquest.app.audio.LocalSoundManager.current
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    soundManager?.playTaskMissedSound()
                }
                PixelCard(
                    variant = PixelPanelVariant.BORDER,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💔 STREAK BROKEN — START A NEW QUEST STREAK TODAY!",
                        style = PixelTypography.bodySmall,
                        color = com.pixelquest.app.ui.theme.PixelRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        if (state.isPerfectDay) {
            item {
                PixelPerfectDayBanner()
            }
        }
        if (state.tasks.isEmpty()) {
            item {
                EmptyTodayState(onCreateQuestClick = onCreateQuestClick)
            }
        } else if (pendingTasks.isNotEmpty()) {
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
