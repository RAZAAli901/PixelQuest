package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.format.DateTimeFormatter

@Composable
fun PixelTaskListItem(
    task: TaskEntity,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    val categoryIcon = when (task.category) {
        TaskCategory.FITNESS -> "💪"
        TaskCategory.PRODUCTIVITY -> "📚"
        TaskCategory.HEALTH -> "❤️"
        TaskCategory.MINDFULNESS -> "🧘"
        TaskCategory.CUSTOM -> "⭐"
    }

    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = categoryIcon,
                style = PixelTypography.displaySmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.name,
                    style = PixelTypography.titleMedium,
                    color = PixelGold
                )
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
