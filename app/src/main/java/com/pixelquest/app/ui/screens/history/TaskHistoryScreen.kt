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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TaskHistoryItem(
    val logId: Long,
    val taskId: Long,
    val taskName: String,
    val category: TaskCategory,
    val completedDate: LocalDate,
    val wasCompleted: Boolean,
    val pointsAwarded: Int
)

@Composable
fun TaskHistoryListItem(
    item: TaskHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    PixelCard(
        variant = if (item.wasCompleted) PixelPanelVariant.BEIGE else PixelPanelVariant.BLUE,
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
                tint = PixelGold,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.taskName,
                    style = PixelTypography.titleMedium,
                    color = PixelTextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.completedDate.format(dateFormatter).uppercase(),
                    style = PixelTypography.labelSmall,
                    color = PixelCyan
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (item.wasCompleted) "COMPLETED" else "MISSED",
                    style = PixelTypography.labelSmall,
                    color = if (item.wasCompleted) PixelGreen else PixelRed
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+${item.pointsAwarded} XP",
                    style = PixelTypography.labelMedium,
                    color = PixelGold
                )
            }
        }
    }
}
