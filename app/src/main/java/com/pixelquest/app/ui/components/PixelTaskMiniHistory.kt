package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PixelTaskMiniHistory(
    recentHistory: List<Pair<LocalDate, Boolean>>,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val dayFormatter = DateTimeFormatter.ofPattern("d")

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "📅 RECENT HISTORY (LAST 14 LOGS)",
            style = MaterialTheme.typography.labelMedium,
            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            recentHistory.takeLast(14).forEach { (date, wasCompleted) ->
                // Read as one item: "done" or "missed", then the day.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    PixelHeatmapCell(
                        status = if (wasCompleted) DailyStatus.PERFECT else DailyStatus.MISSED,
                        size = 18.dp,
                        contentDescription = if (wasCompleted) "done" else "missed"
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date.format(dayFormatter),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = com.pixelquest.app.ui.theme.inkOnPanel(colors.secondary)
                    )
                }
            }
        }
    }
}
