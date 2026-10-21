package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.ui.components.DayStatus
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val WeekdayFormatter = DateTimeFormatter.ofPattern("EEEEE")
private val SpokenDayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM")

/** How a prayer day went, as its dot shows it: all five, some, none and gone, or still going. */
fun dayStatusOf(statuses: List<PrayerStatus>): DayStatus {
    val prayed = statuses.count { it == PrayerStatus.PRAYED }
    return when {
        statuses.isEmpty() -> DayStatus.None
        prayed >= NamazUiState.PRAYERS_PER_DAY -> DayStatus.Perfect
        prayed > 0 -> DayStatus.Partial
        statuses.any { it == PrayerStatus.MISSED } -> DayStatus.Missed
        else -> DayStatus.None
    }
}

/**
 * The last week's prayer days (UI v2, mock 2E), one tab each, today last: its weekday's letter, its
 * date, and a dot for how it went. Picking an earlier one shows its five prayers, read-only; today
 * goes back to marking. The day shown is inked.
 */
@Composable
fun NamazDayStrip(
    days: List<LocalDate>,
    today: LocalDate?,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    week: Map<LocalDate, List<PrayerStatus>> = emptyMap()
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup().testTag(NamazTestTags.DAY_STRIP),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { date ->
            val isSelected = date == selected
            val statuses = week[date].orEmpty()
            val prayed = statuses.count { it == PrayerStatus.PRAYED }
            val spoken = pluralStringResource(R.plurals.namaz_day_spoken, prayed, date.format(SpokenDayFormatter), prayed, NamazUiState.PRAYERS_PER_DAY)
            val bg = if (isSelected) PqColors.Ink else PqColors.Paper
            val fg = if (isSelected) PqColors.Paper else PqColors.Ink
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(pqShape(12.dp))
                    .background(bg)
                    .border(PqDimens.BorderThin, PqColors.Ink, pqShape(12.dp))
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(date) })
                    .semantics { contentDescription = spoken }
                    .testTag(NamazTestTags.day(date)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
            ) {
                Text(
                    date.format(WeekdayFormatter),
                    style = PqType.NavLabel.copy(color = fg), maxLines = 1
                )
                Text(date.dayOfMonth.toString(), style = PqType.BodyStrong.copy(fontSize = 16.bsp, color = fg))
                val status = dayStatusOf(statuses)
                val dot = when {
                    isSelected && date == today -> PqColors.Gold
                    status == DayStatus.None -> Color.Transparent
                    else -> status.color
                }
                Box(Modifier.size(8.dp).clip(PqCircle).background(dot))
            }
        }
    }
}
