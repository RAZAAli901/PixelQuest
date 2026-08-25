package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import java.time.DayOfWeek
import java.time.LocalDate

data class HeatmapDayData(
    val date: LocalDate,
    val status: DailyStatus,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

@Composable
fun PixelCalendarHeatmap(
    statusMap: Map<LocalDate, DailyStatus>,
    modifier: Modifier = Modifier,
    startDate: LocalDate = LocalDate.now().minusMonths(3),
    endDate: LocalDate = LocalDate.now(),
    onDayClick: ((LocalDate, DailyStatus) -> Unit)? = null
) {
    // Group dates into week columns (Monday..Sunday)
    var firstMonday = startDate
    while (firstMonday.dayOfWeek != DayOfWeek.MONDAY) {
        firstMonday = firstMonday.minusDays(1)
    }

    val weeks = mutableListOf<List<LocalDate>>()
    var curr = firstMonday
    while (!curr.isAfter(endDate)) {
        val week = (0..6).map { curr.plusDays(it.toLong()) }
        weeks.add(week)
        curr = curr.plusDays(7)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(8.dp)
    ) {
        weeks.forEach { week ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    val status = statusMap[date] ?: DailyStatus.NO_TASKS_SCHEDULED
                    PixelHeatmapCell(
                        status = status,
                        onClick = if (onDayClick != null) { { onDayClick(date, status) } } else null
                    )
                }
            }
        }
    }
}
