package com.pixelquest.app.study.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * One item, as a card: its kind and title, its subject, when it's due, whether the leader pinned
 * it, how many in the party have finished it, and the player's own tick (done, still to send, or
 * refused). Tapping it opens the item.
 */
@Composable
fun ItemCard(
    row: ItemRow,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val colors = PixelTheme.colors
    val item = row.item
    PixelCard(
        variant = if (row.done) PixelPanelVariant.BEIGE else PixelPanelVariant.BORDER,
        contentPadding = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_open_item), onClick = onOpen)
            .testTag(StudyTags.item(item.id))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(text = kindLabel(item.kind), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                item.subject?.let { Text(text = it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)) }
                if (item.pinned) {
                    val pinned = stringResource(R.string.study_pinned)
                    Text(text = "📌", style = MaterialTheme.typography.labelMedium, modifier = Modifier.clearAndSetSemantics { contentDescription = pinned })
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = inkOnPanel(colors.primaryText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dueText(row.due, item.dueAt, zone),
                style = MaterialTheme.typography.bodySmall,
                color = if (row.due == DueState.OVERDUE) colors.error else colors.onSurface,
                modifier = Modifier.testTag(StudyTags.itemDue(item.id))
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.study_item_done_count, row.doneCount, row.memberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                row.mine?.let { mine ->
                    Text(
                        text = stringResource(
                            when (mine.state) {
                                SyncState.SYNCED -> R.string.study_item_done
                                SyncState.PENDING -> R.string.study_item_waiting
                                SyncState.FAILED -> R.string.study_item_refused
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (mine.state == SyncState.FAILED) colors.error else colors.primaryText,
                        modifier = Modifier.testTag(StudyTags.itemMine(item.id))
                    )
                }
            }
        }
    }
}

@Composable
fun kindLabel(kind: StudyItemKind): String = stringResource(
    when (kind) {
        StudyItemKind.ASSIGNMENT -> R.string.study_kind_assignment
        StudyItemKind.QUIZ -> R.string.study_kind_quiz
    }
)

/** "Due Fri 23 Oct, 17:00", "Due today, 17:00", "Overdue: due Mon 19 Oct, 09:00" or "No due date". */
@Composable
fun dueText(due: DueState, dueAt: Instant?, zone: ZoneId): String {
    if (dueAt == null) return stringResource(R.string.study_section_no_date)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val at = dueAt.atZone(zone)
    val time = at.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    val day = at.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
    return when (due) {
        DueState.TODAY -> stringResource(R.string.study_due_today, time)
        DueState.OVERDUE -> stringResource(R.string.study_due_overdue, day, time)
        else -> stringResource(R.string.study_due_at, day, time)
    }
}
