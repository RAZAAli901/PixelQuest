package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PixelTaskMiniHistory(
    recentHistory: List<Pair<LocalDate, Boolean>>,
    modifier: Modifier = Modifier
) {
    val dayFormatter = DateTimeFormatter.ofPattern("d")

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "📅 RECENT HISTORY (LAST 14 LOGS)",
            style = PixelTypography.labelMedium,
            color = PixelGold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            recentHistory.takeLast(14).forEach { (date, wasCompleted) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelHeatmapCell(
                        status = if (wasCompleted) DailyStatus.PERFECT else DailyStatus.MISSED,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date.format(dayFormatter),
                        style = PixelTypography.labelSmall.copy(fontSize = 7.sp),
                        color = PixelCyan
                    )
                }
            }
        }
    }
}
