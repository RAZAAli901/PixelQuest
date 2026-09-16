package com.pixelquest.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TaskHistoryItem(
    val logId: Long,
    val taskId: Long,
    val taskName: String,
    val category: TaskCategory,
    val completedDate: LocalDate,
    val pointsAwarded: Int,
    val wasCompleted: Boolean
)

@Composable
fun TaskHistoryListItem(
    item: TaskHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    PixelCard(
        variant = if (item.wasCompleted) PixelPanelVariant.BEIGE else PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = item.category.iconResId),
                contentDescription = item.category.displayName,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.taskName,
                    style = PixelTypography.titleMedium,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.completedDate.format(dateFormatter).uppercase(),
                    style = PixelTypography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (item.wasCompleted) "COMPLETED" else "MISSED",
                    style = PixelTypography.labelSmall,
                    color = if (item.wasCompleted) colors.tertiary else colors.error
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+${item.pointsAwarded} XP",
                    style = PixelTypography.labelMedium,
                    color = colors.gold
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📜",
            style = PixelTypography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NO QUEST HISTORY",
            style = PixelTypography.titleMedium,
            color = colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Complete your daily quests to build your log history!",
            style = PixelTypography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun TaskHistoryScreen(
    viewModel: TaskHistoryViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onTaskClick: (Long) -> Unit = {}
) {
    val colors = PixelTheme.colors
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Text(
            text = "📜 QUEST HISTORY LOG",
            style = PixelTypography.titleLarge,
            color = colors.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        com.pixelquest.app.ui.components.PixelFilterChips(
            selectedFilter = state.selectedFilter,
            onFilterSelected = { viewModel.setFilter(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.items.isEmpty()) {
            EmptyHistoryState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = state.items, key = { it.logId }) { item ->
                    TaskHistoryListItem(
                        item = item,
                        onClick = { onTaskClick(item.taskId) }
                    )
                }

                if (state.hasMoreItems) {
                    item {
                        com.pixelquest.app.ui.components.PixelButton(
                            text = "LOAD MORE QUESTS",
                            onClick = { viewModel.loadNextPage() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
