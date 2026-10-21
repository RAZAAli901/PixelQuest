package com.pixelquest.app.study.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.ReminderAudience
import com.pixelquest.app.study.domain.ReminderOccurrence
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** A reminder on This week: its occurrence this week, and whether it's the player's own. */
data class ReminderLine(val reminder: StudyReminder, val occurrence: ReminderOccurrence, val mine: Boolean)

/**
 * The week's reminders, in a banner across the top of This week ("Quiz this week"): each with
 * its type, title and when. One the player may change opens its Edit sheet. Nothing when none.
 */
@Composable
fun ReminderBanner(lines: List<ReminderLine>, today: LocalDate, zone: ZoneId, canEdit: (ReminderLine) -> Boolean, onOpen: (ReminderLine) -> Unit) {
    if (lines.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .comic(bg = PqColors.Sky, radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(ReminderTags.BANNER)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.study_reminder_banner_title).uppercase(),
                style = PqType.MetaStrong,
                modifier = Modifier.semantics { heading() }
            )
        }
        lines.forEach { line ->
            val editable = canEdit(line)
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .then(
                        if (editable) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_reminder_edit)) { onOpen(line) }
                        else Modifier
                    )
                    .padding(vertical = 2.dp)
                    .testTag(ReminderTags.line(line.reminder.id))
            ) {
                Text(
                    text = stringResource(R.string.study_reminder_banner_line, stringResource(reminderKindLabel(line.reminder.kind)), line.reminder.title),
                    style = PqType.BodyStrong,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reminderMeta(line, today, zone),
                    style = PqType.Meta.copy(color = if (line.reminder.sync == SyncState.FAILED) PqColors.Error else PqColors.Ink2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** "All week", "Thu 22 Oct", "Today, 10:00"; then whether it's just the player's, or still waiting to go. */
@Composable
private fun reminderMeta(line: ReminderLine, today: LocalDate, zone: ZoneId): String {
    val locale = LocalConfiguration.current.locales[0]
    val start = line.occurrence.start.withZoneSameInstant(zone)
    val day = when (start.toLocalDate()) {
        today -> stringResource(R.string.study_section_today)
        today.plusDays(1) -> stringResource(R.string.study_section_tomorrow)
        else -> start.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    }
    val whenText = when (line.reminder.scope) {
        ReminderScope.WEEK -> stringResource(R.string.study_reminder_all_week)
        ReminderScope.DAY -> day
        ReminderScope.TIME -> stringResource(R.string.study_reminder_day_time, day, start.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)))
    }
    val extras = listOfNotNull(
        stringResource(R.string.study_reminder_just_you).takeIf { line.reminder.audience == ReminderAudience.ME },
        stringResource(R.string.study_reminder_waiting).takeIf { line.reminder.sync == SyncState.PENDING },
        stringResource(R.string.study_reminder_refused).takeIf { line.reminder.sync == SyncState.FAILED }
    )
    return (listOf(whenText) + extras).joinToString(" · ")
}
