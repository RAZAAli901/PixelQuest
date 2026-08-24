package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun TodayQuestCard(
    task: TaskEntity,
    status: TaskItemStatus,
    onQuickComplete: () -> Unit,
    onQuickSkip: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = status == TaskItemStatus.DONE || status == TaskItemStatus.COMPLETED
    val isMissed = status == TaskItemStatus.MISSED

    val cardVariant = when {
        isDone -> PixelPanelVariant.BLUE
        isMissed -> PixelPanelVariant.BORDER
        else -> PixelPanelVariant.BEIGE
    }

    PixelCard(
        variant = cardVariant,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isMissed) 0.7f else 1.0f)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = task.category.iconResId),
                contentDescription = task.category.displayName,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(28.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.name,
                    style = PixelTypography.titleMedium,
                    color = when {
                        isDone -> PixelGreen
                        isMissed -> PixelRed
                        else -> PixelGold
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ ${task.scheduledTime}",
                        style = PixelTypography.bodySmall,
                        color = PixelCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.category.displayName,
                        style = PixelTypography.labelSmall,
                        color = PixelTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isDone -> {
                    PixelCard(
                        variant = PixelPanelVariant.GREEN,
                        contentPadding = 6.dp
                    ) {
                        Text(
                            text = "✓ DONE",
                            style = PixelTypography.labelMedium,
                            color = PixelBackgroundDark
                        )
                    }
                }
                isMissed -> {
                    PixelCard(
                        variant = PixelPanelVariant.RED,
                        contentPadding = 6.dp
                    ) {
                        Text(
                            text = "✗ MISSED",
                            style = PixelTypography.labelMedium,
                            color = PixelRed
                        )
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelCountdownTimer(
                            scheduledTime = task.scheduledTime,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        PixelButton(
                            text = "✓",
                            onClick = onQuickComplete,
                            variant = PixelButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }
    }
}
