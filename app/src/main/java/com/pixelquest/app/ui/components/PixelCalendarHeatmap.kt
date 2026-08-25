package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HeatmapDayData(
    val date: LocalDate,
    val status: DailyStatus,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun PixelCalendarHeatmap(
    statusMap: Map<LocalDate, DailyStatus>,
    modifier: Modifier = Modifier,
    startDate: LocalDate = LocalDate.now().minusMonths(3),
    endDate: LocalDate = LocalDate.now(),
    onDayClick: ((LocalDate, DailyStatus) -> Unit)? = null
) {
    var selectedDay by remember { mutableStateOf<Pair<LocalDate, DailyStatus>?>(null) }

    if (selectedDay != null) {
        val (date, status) = selectedDay!!
        PixelDayDetailDialog(
            date = date,
            status = status,
            onDismiss = { selectedDay = null }
        )
    }
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

    val monthFormatter = DateTimeFormatter.ofPattern("MMM")

    Column(modifier = modifier.padding(8.dp)) {
        // Month labels row
        Row {
            Spacer(modifier = Modifier.width(28.dp)) // Space for day labels column
            weeks.forEachIndexed { index, week ->
                val firstDayOfWeek = week.first()
                val showMonthLabel = index == 0 || firstDayOfWeek.dayOfMonth <= 7
                Box(
                    modifier = Modifier.width(18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (showMonthLabel) {
                        Text(
                            text = firstDayOfWeek.format(monthFormatter).uppercase(),
                            style = PixelTypography.labelSmall.copy(fontSize = 7.sp),
                            color = PixelGold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Heatmap Grid with Day-of-week labels
        Row {
            // Day of week labels column (Mon, Wed, Fri)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(28.dp)
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier.size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i % 2 == 0) {
                            Text(
                                text = label,
                                style = PixelTypography.labelSmall.copy(fontSize = 8.sp),
                                color = PixelTextWhite,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Week columns
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { date ->
                            val status = statusMap[date] ?: DailyStatus.NO_TASKS_SCHEDULED
                            PixelHeatmapCell(
                                status = status,
                                onClick = {
                                    selectedDay = Pair(date, status)
                                    onDayClick?.invoke(date, status)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
