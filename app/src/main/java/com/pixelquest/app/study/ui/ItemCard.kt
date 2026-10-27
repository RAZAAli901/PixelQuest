package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.CheckCircle
import com.pixelquest.app.ui.components.InitialAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqTag
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * One item, as a row (mock 4H): its kind's tile, its title (and a pin, if the leader pinned it), its
 * kind, subject and when it's due, how many in the party have finished it, and the player's own tick
 * (done, still to send, or refused) in the circle at the end: tick it off, take a tick back within 24
 * hours, or, refused, try again or drop it. Tapping the row opens the item. For its creator or the
 * leader, a ⋯ menu edits, pins (the leader) or deletes it. Once done, it sinks.
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
    onDiscard: (() -> Unit)? = null,
) {
    val item = row.item
    val surface = if (row.done) {
        Modifier.comic(bg = PqColors.Sunken, radius = PqDimens.Radius, shadow = 0.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.study_open_item), onClick = onOpen)
    } else {
        Modifier.comicPressable(onOpen, radius = PqDimens.Radius, onClickLabel = stringResource(R.string.study_open_item), role = Role.Button)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(StudyTags.item(item.id))
            .then(surface)
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
    ) {
        CategoryTile(kindIcon(item.kind), kindTint(item.kind))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    style = PqType.BodyStrong.copy(color = if (row.done) PqColors.Muted else PqColors.Ink),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.pinned) Icon(Icons.Rounded.PushPin, stringResource(R.string.study_pinned), tint = PqColors.RedText, modifier = Modifier.size(16.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (row.due == DueState.OVERDUE && !row.done) PqTag(stringResource(R.string.study_section_overdue))
                Text(
                    text = listOfNotNull(kindLabel(item.kind), item.subject).joinToString(" · "),
                    style = PqType.Meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = dueText(row.due, item.dueAt, zone),
                style = PqType.Meta.copy(color = if (row.due == DueState.OVERDUE) PqColors.Error else PqColors.Muted),
                modifier = Modifier.testTag(StudyTags.itemDue(item.id)),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (row.doneUsers.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-6).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag(StudyTags.itemAvatars(item.id)),
                    ) {
                        row.doneUsers.take(4).forEach { name ->
                            InitialAvatar(name = name, size = 18.dp, bg = PqColors.Sky)
                        }
                    }
                }
                if (row.everyoneDone) {
                    Box(Modifier.testTag(StudyTags.EVERYONE_DONE)) {
                        PqTag(stringResource(R.string.study_item_everyone_done))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.study_item_done_count, row.doneCount, row.memberCount),
                        style = PqType.Meta,
                        modifier = Modifier.testTag(StudyTags.itemDoneCount(item.id)),
                    )
                }
                row.mine?.let { mine ->
                    Text(
                        text = stringResource(
                            when (mine.state) {
                                SyncState.SYNCED -> R.string.study_item_done
                                SyncState.PENDING -> R.string.study_item_waiting
                                SyncState.FAILED -> R.string.study_item_refused
                            },
                        ),
                        style = PqType.MetaStrong.copy(color = if (mine.state == SyncState.FAILED) PqColors.Error else PqColors.RedText),
                        modifier = Modifier.testTag(StudyTags.itemMine(item.id)),
                    )
                }
            }
            if (row.mine?.state == SyncState.FAILED) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    onRetry?.let { CardTextButton(stringResource(R.string.study_try_again), StudyTags.itemRetry(item.id), it) }
                    onDiscard?.let { CardTextButton(stringResource(R.string.study_drop), StudyTags.itemDiscard(item.id), it) }
                }
            }
        }
        if (onEdit != null || onTogglePin != null || onDelete != null) ItemMenu(item.id, item.pinned, onEdit, onTogglePin, onDelete)
        DoneCircle(row, onDone, onUndo)
    }
}

/**
 * The player's own tick, as the circle at the end of the row: empty to tick it off; filled once
 * ticked, and a tap takes it back (one still waiting, or sent within 24 hours). A refused tick, or one
 * older than a day, can't be changed here.
 */
@Composable
private fun DoneCircle(row: ItemRow, onDone: (() -> Unit)?, onUndo: (() -> Unit)?) {
    val id = row.item.id
    val mine = row.mine
    when {
        mine == null -> onDone?.let {
            CheckCircle(false, it, size = 38.dp, modifier = Modifier.testTag(StudyTags.itemDone(id)), label = stringResource(R.string.study_mark_done))
        }
        mine.state == SyncState.FAILED -> Unit
        row.canUndo && onUndo != null -> CheckCircle(true, onUndo, size = 38.dp, modifier = Modifier.testTag(StudyTags.itemUndo(id)), label = stringResource(R.string.study_undo))
        else -> Box(Modifier.clearAndSetSemantics { }) { CheckCircle(true, {}, size = 38.dp) }
    }
}

/**
 * On the item's own screen, what the player can do about their tick: tick it off; take it back (one
 * still waiting, or sent within 24 hours); or, refused, try again or drop it. A tick older than a
 * day is final: nothing to do.
 */
@Composable
internal fun DoneActions(row: ItemRow, onDone: (() -> Unit)?, onUndo: (() -> Unit)?, onRetry: (() -> Unit)?, onDiscard: (() -> Unit)?) {
    val id = row.item.id
    val mine = row.mine
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        when {
            mine == null -> onDone?.let {
                PqButton(text = stringResource(R.string.study_mark_done), onClick = it, icon = Icons.Rounded.Check, modifier = Modifier.weight(1f).testTag(StudyTags.itemDone(id)))
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
    Text(
        text = text,
        style = PqType.MetaStrong.copy(color = PqColors.RedText),
        modifier = Modifier
            .testTag(tag)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 6.dp, vertical = 10.dp),
    )
}

/** An item's kind as its row's tile shows it: a sheet for an assignment, a question for a quiz. */
fun kindIcon(kind: StudyItemKind): ImageVector = when (kind) {
    StudyItemKind.ASSIGNMENT -> Icons.Rounded.Description
    StudyItemKind.QUIZ -> Icons.Rounded.Quiz
}

/** An item's kind's colour: amber for an assignment, lilac for a quiz (mock 4H). */
fun kindTint(kind: StudyItemKind): Color = when (kind) {
    StudyItemKind.ASSIGNMENT -> PqColors.Amber
    StudyItemKind.QUIZ -> PqColors.Lilac
}

/** The ⋯ button and its menu: Edit, Pin or Unpin, Delete, as the player may. */
@Composable
internal fun ItemMenu(itemId: String, pinned: Boolean, onEdit: (() -> Unit)?, onTogglePin: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp).testTag(StudyTags.itemMenu(itemId))) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.study_item_actions), tint = PqColors.Ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(PqColors.Paper)) {
            onEdit?.let {
                DropdownMenuItem(text = { Text(stringResource(R.string.study_edit)) }, onClick = {
                    open = false
                    it()
                })
            }
            onTogglePin?.let {
                DropdownMenuItem(text = { Text(stringResource(if (pinned) R.string.study_unpin else R.string.study_pin)) }, onClick = {
                    open = false
                    it()
                })
            }
            onDelete?.let {
                DropdownMenuItem(text = { Text(stringResource(R.string.study_delete)) }, onClick = {
                    open = false
                    it()
                })
            }
        }
    }
}

@Composable
fun kindLabel(kind: StudyItemKind): String = stringResource(
    when (kind) {
        StudyItemKind.ASSIGNMENT -> R.string.study_kind_assignment
        StudyItemKind.QUIZ -> R.string.study_kind_quiz
    },
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
