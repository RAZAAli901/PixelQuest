package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme
import java.time.LocalDate

@Preview(showBackground = true)
@Composable
fun PixelCalendarHeatmapPreview() {
    PixelQuestTheme {
        Box(
            modifier = Modifier
                .background(PixelBackgroundDark)
                .padding(16.dp)
        ) {
            val today = LocalDate.now()
            val sampleData = mutableMapOf<LocalDate, DailyStatus>()

            (0..90).forEach { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val status = when (daysAgo % 5) {
                    0 -> DailyStatus.PERFECT
                    1 -> DailyStatus.PARTIAL
                    2 -> DailyStatus.MISSED
                    3 -> DailyStatus.NO_TASKS_SCHEDULED
                    else -> DailyStatus.PERFECT
                }
                sampleData[date] = status
            }

            PixelCalendarHeatmap(
                statusMap = sampleData,
                startDate = today.minusMonths(3),
                endDate = today
            )
        }
    }
}
