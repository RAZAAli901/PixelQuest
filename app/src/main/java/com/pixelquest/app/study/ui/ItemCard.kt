package com.pixelquest.app.study.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.SymbolIcon
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
 * refused), with a bar for the party's progress on it and a button to tick it off, take a tick
 * back within 24 hours, or try a refused one again. Tapping it opens the item. For its creator or the leader, a ⋯ menu edits, pins (the
 * leader) or deletes it.
 */
@Composable
fun ItemCard(
    row: ItemRow,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    onEdit: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onDiscard: (() -> Unit)? = null
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
                if (onEdit != null || onTogglePin != null || onDelete != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    ItemMenu(item.id, item.pinned, onEdit, onTogglePin, onDelete)
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
            if (row.memberCount > 0) {
                PixelProgressBar(progress = row.doneCount.toFloat() / row.memberCount, height = 8.dp, modifier = Modifier.fillMaxWidth())
            }
            DoneActions(row, onDone, onUndo, onRetry, onDiscard)
        }
    }
}

/**
 * What the player can do about their own tick: tick it off; take it back (one still waiting, or sent
 * within 24 hours); or, refused, try again or drop it. A tick older than a day is final: no button.
 */
@Composable
private fun DoneActions(row: ItemRow, onDone: (() -> Unit)?, onUndo: (() -> Unit)?, onRetry: (() -> Unit)?, onDiscard: (() -> Unit)?) {
    val id = row.item.id
    val mine = row.mine
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        when {
            mine == null -> onDone?.let {
                PixelButton(text = stringResource(R.string.study_mark_done), onClick = it, modifier = Modifier.weight(1f).testTag(StudyTags.itemDone(id)))
            }
            mine.state == SyncState.FAILED -> {
                onRetry?.let { CardTextButton(stringResource(R.string.study_try_again), StudyTags.itemRetry(id), it) }
                onDiscard?.let { CardTextButton(stringResource(R.string.study_drop), StudyTags.itemDiscard(id), it) }
            }
            row.canUndo -> onUndo?.let { CardTextButton(stringResource(R.string.study_undo), StudyTags.itemUndo(id), it) }
        }
    }
}

@Composable
private fun CardTextButton(text: String, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = PixelTheme.colors.primaryText)
    }
}

/** The ⋯ button and its menu: Edit, Pin or Unpin, Delete, as the player may. */
@Composable
private fun ItemMenu(itemId: String, pinned: Boolean, onEdit: (() -> Unit)?, onTogglePin: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag(StudyTags.itemMenu(itemId))) {
            SymbolIcon(symbol = "⋯", contentDescription = stringResource(R.string.study_item_actions), style = MaterialTheme.typography.titleMedium, color = PixelTheme.colors.onSurface)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            onEdit?.let { DropdownMenuItem(text = { Text(stringResource(R.string.study_edit)) }, onClick = { open = false; it() }) }
            onTogglePin?.let {
                DropdownMenuItem(text = { Text(stringResource(if (pinned) R.string.study_unpin else R.string.study_pin)) }, onClick = { open = false; it() })
            }
            onDelete?.let { DropdownMenuItem(text = { Text(stringResource(R.string.study_delete)) }, onClick = { open = false; it() }) }
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
