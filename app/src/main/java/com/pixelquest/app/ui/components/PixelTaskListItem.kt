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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.format.DateTimeFormatter

enum class TaskItemStatus {
    PENDING,
    COMPLETED,
    DONE,
    MISSED,
    GRACE_PERIOD
}

@Composable
fun PixelTaskListItem(
    task: TaskEntity,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    status: TaskItemStatus = TaskItemStatus.PENDING,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val cardVariant = when (status) {
        TaskItemStatus.COMPLETED, TaskItemStatus.DONE -> PixelPanelVariant.BLUE
        TaskItemStatus.MISSED -> PixelPanelVariant.BORDER
        TaskItemStatus.PENDING -> PixelPanelVariant.BEIGE
    }

    PixelCard(
        variant = cardVariant,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (status == TaskItemStatus.MISSED) 0.7f else 1.0f)
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
                    .size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.name,
                        style = PixelTypography.titleMedium,
                        color = when (status) {
                            TaskItemStatus.COMPLETED -> PixelGreen
                            TaskItemStatus.MISSED -> PixelRed
                            TaskItemStatus.PENDING -> PixelGold
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (status == TaskItemStatus.COMPLETED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(" [DONE]", style = PixelTypography.labelSmall, color = PixelGreen)
                    } else if (status == TaskItemStatus.MISSED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(" [MISSED]", style = PixelTypography.labelSmall, color = PixelRed)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ ${task.scheduledTime.format(timeFormatter)}",
                        style = PixelTypography.bodySmall,
                        color = PixelCyan
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "🔄 ${task.recurrenceType.name}",
                        style = PixelTypography.labelSmall,
                        color = PixelTextMuted
                    )
                }
            }
            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick) {
                    Text("🗑️", style = PixelTypography.titleMedium)
                }
            }
        }
    }
}
