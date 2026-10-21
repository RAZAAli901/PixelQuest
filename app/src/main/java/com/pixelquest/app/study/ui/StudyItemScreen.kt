package com.pixelquest.app.study.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
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
    val colors = PixelTheme.colors
    val row = state.items.firstOrNull { it.item.id == itemId }
    // Once shown, an item that goes (deleted here or by someone else) closes its screen.
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(row != null) {
        if (row != null) seen = true else if (seen) onNavigateBack()
    }
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = row?.let { kindLabel(it.item.kind) } ?: stringResource(R.string.party_study_title),
                subtitle = state.party?.name,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(symbol = "◀", contentDescription = stringResource(R.string.navigate_back), style = MaterialTheme.typography.titleMedium, color = colors.primaryText)
                    }
                },
                actions = {
                    if (row != null && (row.canEdit || state.isLeader)) ItemTopMenu(row, state.isLeader, actions)
                }
            )
        },
        modifier = Modifier.testTag(StudyTags.ITEM_SCREEN)
    ) { innerPadding ->
        state.confirmingDelete?.let { deleting ->
            PixelConfirmDialog(
                title = stringResource(R.string.study_delete_title),
                message = stringResource(R.string.study_delete_message, deleting.item.title),
                confirmText = stringResource(R.string.study_delete),
                dismissText = stringResource(R.string.study_keep),
                onConfirm = onConfirmDelete,
                onDismiss = onCancelDelete
            )
        }
        state.form?.let { form ->
            ItemSheet(form = form, today = state.today, isLeader = state.isLeader, onChange = onFormChange, onSave = onSaveForm, onDismiss = onCloseForm)
        }
        if (row == null) {
            Text(
                text = stringResource(if (state.loaded) R.string.study_item_gone else R.string.study_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
            return@Scaffold
        }
        val item = row.item
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            PixelCard(variant = PixelPanelVariant.BEIGE, contentPadding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.subject?.let { Text(text = it, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
                    Text(text = item.title, style = MaterialTheme.typography.headlineSmall, color = inkOnPanel(colors.primaryText), modifier = Modifier.semantics { heading() })
                    Text(
                        text = dueText(row.due, item.dueAt, zone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (row.due == DueState.OVERDUE) colors.error else colors.onSurface,
                        modifier = Modifier.testTag(StudyTags.itemDue(item.id))
                    )
                    if (item.pinned) Text(text = stringResource(R.string.study_pinned_line, stringResource(R.string.study_pinned)), style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                    val adder = state.members.firstOrNull { it.userId == item.createdBy }?.username
                    Text(
                        text = stringResource(R.string.study_item_added_by, adder?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            item.notes?.let { notes ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Heading(stringResource(R.string.study_item_notes))
                    Text(text = notes, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.testTag(StudyTags.ITEM_NOTES))
                }
            }
            DoneActions(
                row = row,
                onDone = { actions.onDone(item.id) },
                onUndo = { actions.onUndo(item.id) },
                onRetry = row.mine?.clientId?.let { key -> { actions.onRetry(key) } },
                onDiscard = row.mine?.clientId?.let { key -> { actions.onDiscard(key) } }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Heading(stringResource(R.string.study_item_who_done))
                Text(text = stringResource(R.string.study_item_done_count, row.doneCount, row.memberCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                if (row.memberCount > 0) PixelProgressBar(progress = row.doneCount.toFloat() / row.memberCount, height = 12.dp, modifier = Modifier.fillMaxWidth())
                val done = state.ticks[item.id].orEmpty().filter { it.state != SyncState.FAILED }
                if (done.isEmpty()) {
                    Text(text = stringResource(R.string.study_item_nobody_done), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                } else {
                    done.sortedBy { it.completedAt }.forEach { tick -> DoneByLine(state, tick, zone) }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = PixelTheme.colors.primaryText, modifier = Modifier.semantics { heading() })
}

/** One member who finished it: their name, and the day. */
@Composable
private fun DoneByLine(state: StudyPartyUiState, tick: StudyCompletion, zone: ZoneId) {
    val name = state.members.firstOrNull { it.userId == tick.userId }?.username
    val day = tick.completedAt.atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM", LocalConfiguration.current.locales[0]))
    Text(
        text = stringResource(R.string.study_item_done_by, name?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone), day),
        style = MaterialTheme.typography.bodyMedium,
        color = PixelTheme.colors.onSurface,
        modifier = Modifier.testTag(StudyTags.doneBy(tick.userId.orEmpty()))
    )
}

/** The top bar's ⋯: Edit and Delete for its creator or the leader, Pin for the leader. */
@Composable
private fun ItemTopMenu(row: ItemRow, isLeader: Boolean, actions: ItemActions) {
    var open by remember { mutableStateOf(false) }
    val id = row.item.id
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp).testTag(StudyTags.itemMenu(id))) {
            SymbolIcon(symbol = "⋯", contentDescription = stringResource(R.string.study_item_actions), style = MaterialTheme.typography.titleMedium, color = PixelTheme.colors.primaryText)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (row.canEdit) DropdownMenuItem(text = { Text(stringResource(R.string.study_edit)) }, onClick = { open = false; actions.onEdit(id) })
            if (isLeader) {
                DropdownMenuItem(text = { Text(stringResource(if (row.item.pinned) R.string.study_unpin else R.string.study_pin)) }, onClick = { open = false; actions.onTogglePin(id) })
            }
            if (row.canEdit) DropdownMenuItem(text = { Text(stringResource(R.string.study_delete)) }, onClick = { open = false; actions.onDelete(id) })
        }
    }
}
