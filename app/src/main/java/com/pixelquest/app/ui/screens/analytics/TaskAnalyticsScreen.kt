package com.pixelquest.app.ui.screens.analytics

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelStatCard
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TaskAnalyticsContent(
    task: TaskEntity?,
    stats: PerTaskStats,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            PixelButton(
                text = "← BACK",
                onClick = onBackClick
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "QUEST ANALYTICS",
                style = PixelTypography.titleMedium,
                color = PixelGold
            )
        }

        if (task != null) {
            PixelCard(
                variant = PixelPanelVariant.BEIGE,
                contentPadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = task.category.iconResId),
                        contentDescription = task.category.displayName,
                        tint = PixelGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = task.name.uppercase(),
                            style = PixelTypography.titleLarge,
                            color = PixelTextWhite
                        )
                        Text(
                            text = task.category.displayName,
                            style = PixelTypography.bodySmall,
                            color = PixelCyan
                        )
                    }
                }
            }
        }

        // Stats grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PixelStatCard(
                label = "COMPLETIONS",
                value = "${stats.completionCount}",
                icon = "🎯",
                accentColor = PixelGreen,
                modifier = Modifier.weight(1f)
            )
            PixelStatCard(
                label = "COMPLETION RATE",
                value = "${(stats.completionRate * 100).toInt()}%",
                icon = "📊",
                accentColor = PixelCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PixelStatCard(
                label = "CURRENT STREAK",
                value = "${stats.currentStreak} DAYS",
                icon = "🔥",
                accentColor = PixelGold,
                modifier = Modifier.weight(1f)
            )
            PixelStatCard(
                label = "LONGEST STREAK",
                value = "${stats.longestStreak} DAYS",
                icon = "🏆",
                accentColor = PixelGold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            com.pixelquest.app.ui.components.PixelTaskMiniHistory(
                recentHistory = stats.recentHistory
            )
        }
    }
}

@Composable
fun TaskAnalyticsScreen(
    viewModel: TaskAnalyticsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    TaskAnalyticsContent(
        task = state.task,
        stats = state.stats,
        onBackClick = onBackClick
    )
}
