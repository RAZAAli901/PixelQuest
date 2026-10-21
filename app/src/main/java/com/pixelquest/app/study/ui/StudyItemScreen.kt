package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.StudyCompletion
import com.pixelquest.app.study.domain.SyncState
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqProgressBar
import com.pixelquest.app.ui.components.PqTag
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object StudyItemScreenArgs {
    const val ARG_ITEM = "itemId"
}

/**
 * One item's own screen: everything about it (kind, title, subject, when it's due, notes, who added
 * it), how the party is doing on it and who has finished it, and the player's own Done or Undo. Its
 * creator or the leader edits, pins or deletes it from the top bar. It reads the party's screen's
 * state, so both always agree.
 */
@Composable
fun StudyItemScreen(itemId: String, onNavigateBack: () -> Unit, viewModel: StudyPartyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    StudyItemContent(
        state = state,
        itemId = itemId,
        onNavigateBack = onNavigateBack,
        actions = ItemActions(
            onEdit = viewModel::editItem,
            onTogglePin = viewModel::togglePin,
            onDelete = viewModel::askDelete,
            onDone = viewModel::complete,
            onUndo = viewModel::undo,
            onRetry = viewModel::retry,
            onDiscard = viewModel::discard
        ),
        onFormChange = viewModel::updateForm,
        onSaveForm = viewModel::saveForm,
        onCloseForm = viewModel::closeForm,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete
    )
}

@Composable
fun StudyItemContent(
    state: StudyPartyUiState,
    itemId: String,
    onNavigateBack: () -> Unit,
    actions: ItemActions = ItemActions(),
    onFormChange: (ItemForm) -> Unit = {},
    onSaveForm: () -> Unit = {},
    onCloseForm: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault()
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalStudySimpleMode provides state.simpleMode) {
        StudyItemBody(state, itemId, onNavigateBack, actions, onFormChange, onSaveForm, onCloseForm, onConfirmDelete, onCancelDelete, zone)
    }
}

@Composable
private fun StudyItemBody(
    state: StudyPartyUiState,
    itemId: String,
    onNavigateBack: () -> Unit,
    actions: ItemActions,
    onFormChange: (ItemForm) -> Unit,
    onSaveForm: () -> Unit,
    onCloseForm: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    zone: ZoneId
) {
    val row = state.items.firstOrNull { it.item.id == itemId }
    // Once shown, an item that goes (deleted here or by someone else) closes its screen.
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(row != null) {
        if (row != null) seen = true else if (seen) onNavigateBack()
    }
    state.confirmingDelete?.let { deleting ->
        PqConfirmDialog(
            title = stringResource(R.string.study_delete_title),
            message = stringResource(R.string.study_delete_message, deleting.item.title),
            confirm = stringResource(R.string.study_delete),
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }
    state.form?.let { form ->
        ItemSheet(form = form, today = state.today, isLeader = state.isLeader, onChange = onFormChange, onSave = onSaveForm, onDismiss = onCloseForm)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(PqDimens.Gap),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp)
            .testTag(StudyTags.ITEM_SCREEN)
    ) {
        PqTopBar(row?.let { kindLabel(it.item.kind) } ?: stringResource(R.string.party_study_title), onBack = onNavigateBack) {
            if (row != null && (row.canEdit || state.isLeader)) {
                val id = row.item.id
                ItemMenu(
                    itemId = id,
                    pinned = row.item.pinned,
                    onEdit = if (row.canEdit) ({ actions.onEdit(id) }) else null,
                    onTogglePin = if (state.isLeader) ({ actions.onTogglePin(id) }) else null,
                    onDelete = if (row.canEdit) ({ actions.onDelete(id) }) else null
                )
            }
        }
        if (row == null) {
            Text(text = stringResource(if (state.loaded) R.string.study_item_gone else R.string.study_loading), style = PqType.Body.copy(color = PqColors.Muted))
            return@Column
        }
        val item = row.item
        PqCard(Modifier.fillMaxWidth(), shadow = PqDimens.ShadowLg) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                CategoryTile(kindIcon(item.kind), kindTint(item.kind))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(text = listOfNotNull(state.party?.name, item.subject).joinToString(" · "), style = PqType.Meta)
                    Text(text = item.title, style = PqType.CardTitle, modifier = Modifier.semantics { heading() })
                    Text(
                        text = dueText(row.due, item.dueAt, zone),
                        style = PqType.BodyStrong.copy(color = if (row.due == DueState.OVERDUE) PqColors.Error else PqColors.Ink),
                        modifier = Modifier.testTag(StudyTags.itemDue(item.id))
                    )
                    if (item.pinned) PqTag(stringResource(R.string.study_pinned))
                    val adder = state.members.firstOrNull { it.userId == item.createdBy }?.username
                    Text(
                        text = stringResource(R.string.study_item_added_by, adder?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)),
                        style = PqType.Meta
                    )
                }
            }
        }
        item.notes?.let { notes ->
            Heading(stringResource(R.string.study_item_notes))
            PqCard(Modifier.fillMaxWidth()) {
                Text(text = notes, style = PqType.Body, modifier = Modifier.padding(14.dp).testTag(StudyTags.ITEM_NOTES))
            }
        }
        DoneActions(
            row = row,
            onDone = { actions.onDone(item.id) },
            onUndo = { actions.onUndo(item.id) },
            onRetry = row.mine?.clientId?.let { key -> { actions.onRetry(key) } },
            onDiscard = row.mine?.clientId?.let { key -> { actions.onDiscard(key) } }
        )
        Heading(stringResource(R.string.study_item_who_done))
        PqCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.study_item_done_count, row.doneCount, row.memberCount), style = PqType.BodyStrong)
                if (row.memberCount > 0) PqProgressBar(progress = row.doneCount.toFloat() / row.memberCount, modifier = Modifier.fillMaxWidth())
                val done = state.ticks[item.id].orEmpty().filter { it.state != SyncState.FAILED }
                if (done.isEmpty()) {
                    Text(text = stringResource(R.string.study_item_nobody_done), style = PqType.Body.copy(color = PqColors.Muted))
                } else {
                    done.sortedBy { it.completedAt }.forEach { tick -> DoneByLine(state, tick, zone) }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    PqGroupLabel(text, Modifier.semantics { heading() })
}

/** One member who finished it: their name, and the day. */
@Composable
private fun DoneByLine(state: StudyPartyUiState, tick: StudyCompletion, zone: ZoneId) {
    val name = state.members.firstOrNull { it.userId == tick.userId }?.username
    val day = tick.completedAt.atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM", LocalConfiguration.current.locales[0]))
    Text(
        text = stringResource(R.string.study_item_done_by, name?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone), day),
        style = PqType.Body,
        modifier = Modifier.testTag(StudyTags.doneBy(tick.userId.orEmpty()))
    )
}
