package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    isSimpleMode: Boolean = false,
    onDayClick: ((LocalDate, DailyStatus) -> Unit)? = null
) {
    val colors = PixelTheme.colors
    var selectedDay by remember { mutableStateOf<Pair<LocalDate, DailyStatus>?>(null) }

    if (selectedDay != null) {
        val (date, status) = selectedDay!!
        PixelDayDetailDialog(
            date = date,
            status = status,
            isSimpleMode = isSimpleMode,
            onDismiss = { selectedDay = null }
        )
    }
    val weeks = remember(startDate, endDate, statusMap) {
        var firstMonday = startDate
        while (firstMonday.dayOfWeek != DayOfWeek.MONDAY) {
            firstMonday = firstMonday.minusDays(1)
        }

        val list = mutableListOf<List<LocalDate>>()
        var curr = firstMonday
        while (!curr.isAfter(endDate)) {
            val week = (0..6).map { curr.plusDays(it.toLong()) }
            list.add(week)
            curr = curr.plusDays(7)
        }
        list
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
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = com.pixelquest.app.ui.theme.inkOnPanel(colors.primary),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = com.pixelquest.app.ui.theme.inkOnPanel(colors.onSurfaceVariant),
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
                                contentDescription = HeatmapCellLabels.describe(date, status, today = endDate),
                                // The rest of this week's column hasn't happened: nothing to open.
                                onClick = if (date.isAfter(endDate)) null else {
                                    {
                                        selectedDay = Pair(date, status)
                                        onDayClick?.invoke(date, status)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Legend swatches come from the same mapper as the day cells, so they match in every theme.
            val legendMode = PixelTheme.mode
            HeatmapLegendItem(
                color = HeatmapColorMapper.getCellColor(DailyStatus.PERFECT, legendMode),
                borderColor = HeatmapColorMapper.getBorderColor(DailyStatus.PERFECT, legendMode),
                label = if (isSimpleMode) "Completed" else "Perfect Day"
            )
            HeatmapLegendItem(
                color = HeatmapColorMapper.getCellColor(DailyStatus.PARTIAL, legendMode),
                borderColor = HeatmapColorMapper.getBorderColor(DailyStatus.PARTIAL, legendMode),
                label = "Partial"
            )
            HeatmapLegendItem(
                color = HeatmapColorMapper.getCellColor(DailyStatus.MISSED, legendMode),
                borderColor = HeatmapColorMapper.getBorderColor(DailyStatus.MISSED, legendMode),
                label = "Missed"
            )
            HeatmapLegendItem(
                color = HeatmapColorMapper.getCellColor(DailyStatus.NO_TASKS_SCHEDULED, legendMode),
                borderColor = HeatmapColorMapper.getBorderColor(DailyStatus.NO_TASKS_SCHEDULED, legendMode),
                label = if (isSimpleMode) "No Tasks" else "No Quests"
            )
        }
    }
}

@Composable
private fun HeatmapLegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    borderColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color)
                .border(1.dp, borderColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
            color = com.pixelquest.app.ui.theme.inkOnPanel(PixelTheme.colors.onSurfaceVariant)
        )
    }
}

