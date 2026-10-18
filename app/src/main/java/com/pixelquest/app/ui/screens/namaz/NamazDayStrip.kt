package com.pixelquest.app.ui.screens.namaz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val WeekdayFormatter = DateTimeFormatter.ofPattern("EEE")
private val SpokenDayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM")

/**
 * The last week's prayer days, one tab each, today last, each with five dots for its prayers:
 * the week at a glance. Picking an earlier one shows its five prayers, read-only; today goes back
 * to marking.
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
    val colors = PixelTheme.colors
    val shape = if (PixelTheme.mode == ThemeMode.Pixel) RectangleShape else RoundedCornerShape(8.dp)
    Column(modifier = modifier.testTag(NamazTestTags.DAY_STRIP), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.namaz_days_label), style = MaterialTheme.typography.titleSmall, color = colors.primaryText)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.forEach { date ->
                val isSelected = date == selected
                val statuses = week[date].orEmpty()
                val spoken = stringResource(
                    R.string.namaz_day_spoken,
                    date.format(SpokenDayFormatter),
                    statuses.count { it == PrayerStatus.PRAYED },
                    NamazUiState.PRAYERS_PER_DAY
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(shape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surface, shape)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.primaryText else colors.pixelBorder,
                            shape = shape
                        )
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(date) })
                        .semantics { contentDescription = spoken }
                        .padding(vertical = 8.dp)
                        .testTag(NamazTestTags.day(date)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (date == today) stringResource(R.string.namaz_day_today) else date.format(WeekdayFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (statuses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PrayerDots(statuses)
                    }
                }
            }
        }
    }
}

/** A day's five prayers as dots, Fajr to Isha: filled for prayed, red for missed, hollow for still to come. */
@Composable
private fun PrayerDots(statuses: List<PrayerStatus>) {
    val colors = PixelTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        statuses.forEach { status ->
            val fill = when (status) {
                PrayerStatus.PRAYED -> colors.tertiary
                PrayerStatus.MISSED -> colors.error
                PrayerStatus.PENDING -> Color.Transparent
            }
            Spacer(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(fill, CircleShape)
                    .border(1.dp, if (status == PrayerStatus.PENDING) colors.onSurfaceVariant else fill, CircleShape)
            )
        }
    }
}
