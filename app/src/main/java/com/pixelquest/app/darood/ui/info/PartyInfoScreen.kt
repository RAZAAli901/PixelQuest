package com.pixelquest.app.darood.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.domain.MemberStatus
import com.pixelquest.app.darood.domain.Party
import com.pixelquest.app.darood.domain.PartyMember
import com.pixelquest.app.darood.domain.PartyRole
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelConfirmDialog
import com.pixelquest.app.ui.components.PixelDialog
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PixelTopAppBar
import com.pixelquest.app.ui.components.SymbolIcon
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/** Why the last change didn't happen. */
sealed interface InfoProblem {
    data object Offline : InfoProblem
    data class Refused(val refusal: DaroodRefusal) : InfoProblem
}

/**
 * @param members the party's active members: the leader first, then in the order they joined.
 * @param myId the signed-in account, to mark it and to know whether it leads.
 * @param confirmingRemoval the member the leader asked to remove, while the dialog asks if they're sure.
 * @param confirmingLeave the player asked to leave, and the dialog asks if they're sure.
 * @param newName the name typed in the rename dialog, while it's open.
 * @param confirmingTransfer the member the leader asked to hand the leadership to, while the dialog asks.
 * @param deleteConfirmation what the leader has typed to confirm deleting the party, while the dialog asks.
 * @param busy a change is on its way to the server.
 * @param left the player is out of the party (they left it, or deleted it): the screen closes.
 */
data class PartyInfoUiState(
    val party: Party? = null,
    val members: List<PartyMember> = emptyList(),
    val myId: String? = null,
    val confirmingRemoval: PartyMember? = null,
    val confirmingLeave: Boolean = false,
    val newName: String? = null,
    val confirmingTransfer: PartyMember? = null,
    val deleteConfirmation: String? = null,
    val busy: Boolean = false,
    val problem: InfoProblem? = null,
    val left: Boolean = false
) {
    val isLeader: Boolean get() = party?.leaderId != null && party.leaderId == myId

    /** The leader has typed the party's name, exactly, to confirm deleting it. */
    val deleteConfirmed: Boolean get() = party != null && deleteConfirmation?.trim() == party.name

    /** The new name's length as it will be stored (without spaces at either end). */
    private val newNameLength: Int get() = newName?.trim()?.length ?: 0

    /** Why the typed name can't be the party's; null while it's fine. */
    val newNameProblem: com.pixelquest.app.darood.ui.create.PartyNameProblem?
        get() = when {
            newName == null -> null
            newNameLength < DaroodRules.NAME_MIN -> com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_SHORT
            newNameLength > DaroodRules.NAME_MAX -> com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_LONG
            else -> null
        }
}

/** A party's info: who's in it, and since when (docs/darood-design.md, Parties). */
@HiltViewModel
class PartyInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DaroodPartyRepository,
    account: DaroodAccount? = null
) : ViewModel() {

    val partyId: String = savedStateHandle["partyId"] ?: ""

    private val _uiState = MutableStateFlow(PartyInfoUiState(myId = account?.id()))
    val uiState: StateFlow<PartyInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeParty(partyId).collect { party -> _uiState.update { it.copy(party = party) } }
        }
        viewModelScope.launch {
            repository.observeMembers(partyId).collect { members ->
                _uiState.update { state ->
                    state.copy(
                        members = members.filter { it.status == MemberStatus.ACTIVE }
                            .sortedWith(compareBy({ it.role != PartyRole.LEADER }, { it.joinedAt ?: Instant.MAX }))
                    )
                }
            }
        }
        viewModelScope.launch { repository.refreshParty(partyId) }
    }

    /** The leader asks to remove [member]: the dialog asks if they're sure. */
    fun askToRemove(member: PartyMember) {
        if (!_uiState.value.isLeader || member.role == PartyRole.LEADER) return
        _uiState.update { it.copy(confirmingRemoval = member, problem = null) }
    }

    fun dismissDialog() = _uiState.update { it.copy(confirmingRemoval = null, confirmingLeave = false, newName = null, confirmingTransfer = null, deleteConfirmation = null) }

    /** The leader opens the rename dialog, with the party's name to edit. */
    fun startRename() {
        val party = _uiState.value.party ?: return
        if (!_uiState.value.isLeader) return
        _uiState.update { it.copy(newName = party.name, problem = null) }
    }

    fun onNewNameChange(name: String) = _uiState.update { if (it.newName == null) it else it.copy(newName = name.take(DaroodRules.NAME_MAX + 10)) }

    /** Saves the typed name, if it can be a party's (2 to 40 characters, as the server insists) and is new. */
    fun confirmRename() {
        val state = _uiState.value
        val name = state.newName?.trim() ?: return
        if (state.busy || state.newNameProblem != null) return
        if (name == state.party?.name) return dismissDialog()
        _uiState.update { it.copy(newName = null, busy = true) }
        viewModelScope.launch {
            val result = repository.renameParty(partyId, name)
            _uiState.update { it.copy(busy = false, problem = problemOf(result)) }
        }
    }

    /** The leader asks to delete the party: the dialog asks for its name, to be sure. */
    fun startDelete() {
        if (!_uiState.value.isLeader) return
        _uiState.update { it.copy(deleteConfirmation = "", problem = null) }
    }

    fun onDeleteConfirmationChange(text: String) =
        _uiState.update { if (it.deleteConfirmation == null) it else it.copy(deleteConfirmation = text.take(DaroodRules.NAME_MAX + 10)) }

    /** Deletes the party, once its name is typed: it's gone for everyone, and the screen closes. */
    fun confirmDelete() {
        val state = _uiState.value
        if (state.busy || !state.deleteConfirmed) return
        _uiState.update { it.copy(deleteConfirmation = null, busy = true) }
        viewModelScope.launch {
            val result = repository.deleteParty(partyId)
            _uiState.update { it.copy(busy = false, problem = problemOf(result), left = result is SupabaseResult.Success) }
        }
    }

    /** The leader asks to hand the leadership to [member]: the dialog asks if they're sure. */
    fun askToTransfer(member: PartyMember) {
        if (!_uiState.value.isLeader || member.role == PartyRole.LEADER) return
        _uiState.update { it.copy(confirmingTransfer = member, problem = null) }
    }

    /** Hands the leadership over. The player stays in the party as a member, and can then leave it. */
    fun confirmTransfer() {
        val member = _uiState.value.confirmingTransfer ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(confirmingTransfer = null, busy = true) }
        viewModelScope.launch {
            val result = repository.transferLeadership(partyId, member.userId)
            _uiState.update { it.copy(busy = false, problem = problemOf(result)) }
        }
    }

    /** A member asks to leave: the dialog asks if they're sure. The leader hands over first, as the server insists. */
    fun askToLeave() {
        if (_uiState.value.isLeader) return
        _uiState.update { it.copy(confirmingLeave = true, problem = null) }
    }

    /** Leaves the party. What the player posted stays in its total. */
    fun confirmLeave() {
        if (!_uiState.value.confirmingLeave || _uiState.value.busy) return
        _uiState.update { it.copy(confirmingLeave = false, busy = true) }
        viewModelScope.launch {
            val result = repository.leaveParty(partyId)
            _uiState.update { it.copy(busy = false, problem = problemOf(result), left = result is SupabaseResult.Success) }
        }
    }

    /** Removes the member the dialog asked about. What they posted stays in the party's total. */
    fun confirmRemoval() {
        val member = _uiState.value.confirmingRemoval ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(confirmingRemoval = null, busy = true) }
        viewModelScope.launch {
            val result = repository.removeMember(partyId, member.userId)
            _uiState.update { it.copy(busy = false, problem = problemOf(result)) }
        }
    }

    private fun problemOf(result: SupabaseResult<*>): InfoProblem? = when (result) {
        is SupabaseResult.Success -> null
        is SupabaseResult.NetworkError -> InfoProblem.Offline
        else -> InfoProblem.Refused(DaroodRefusal.of(result) ?: DaroodRefusal.INVALID)
    }
}

@Composable
fun PartyInfoScreen(
    onNavigateBack: () -> Unit,
    onAddPeople: () -> Unit,
    onLeft: () -> Unit = onNavigateBack,
    viewModel: PartyInfoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.left) { if (state.left) onLeft() }
    PartyInfoContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onAddPeople = onAddPeople,
        onRemove = viewModel::askToRemove,
        onConfirmRemoval = viewModel::confirmRemoval,
        onLeave = viewModel::askToLeave,
        onConfirmLeave = viewModel::confirmLeave,
        onRename = viewModel::startRename,
        onNewNameChange = viewModel::onNewNameChange,
        onConfirmRename = viewModel::confirmRename,
        onTransfer = viewModel::askToTransfer,
        onConfirmTransfer = viewModel::confirmTransfer,
        onDelete = viewModel::startDelete,
        onDeleteConfirmationChange = viewModel::onDeleteConfirmationChange,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDialog = viewModel::dismissDialog
    )
}

@Composable
fun PartyInfoContent(
    state: PartyInfoUiState,
    onNavigateBack: () -> Unit,
    onAddPeople: () -> Unit = {},
    onRemove: (PartyMember) -> Unit = {},
    onConfirmRemoval: () -> Unit = {},
    onLeave: () -> Unit = {},
    onConfirmLeave: () -> Unit = {},
    onRename: () -> Unit = {},
    onNewNameChange: (String) -> Unit = {},
    onConfirmRename: () -> Unit = {},
    onTransfer: (PartyMember) -> Unit = {},
    onConfirmTransfer: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDeleteConfirmationChange: (String) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDialog: () -> Unit = {}
) {
    val colors = PixelTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            PixelTopAppBar(
                title = stringResource(R.string.darood_party_info),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            symbol = "◀",
                            contentDescription = stringResource(R.string.navigate_back),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryText
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag(DaroodTestTags.INFO_SCREEN),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val party = state.party ?: return@Column
            Text(
                text = party.name,
                style = MaterialTheme.typography.titleLarge,
                color = colors.primaryText,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = pluralStringResource(R.plurals.darood_info_size, state.members.size, state.members.size, party.maxMembers),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            if (state.isLeader) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!party.isFinished) {
                        PixelButton(
                            text = stringResource(R.string.darood_add_title),
                            onClick = onAddPeople,
                            modifier = Modifier.testTag(DaroodTestTags.INFO_ADD_PEOPLE)
                        )
                    }
                    PixelButton(
                        text = stringResource(R.string.darood_info_rename),
                        onClick = onRename,
                        variant = PixelButtonVariant.BLUE,
                        enabled = !state.busy,
                        modifier = Modifier.testTag(DaroodTestTags.INFO_RENAME)
                    )
                }
            }
            Text(
                text = stringResource(R.string.darood_info_members),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primaryText,
                modifier = Modifier.semantics { heading() }
            )
            state.problem?.let { ProblemLine(it) }
            state.members.forEach { member ->
                MemberRow(
                    member = member,
                    isMe = member.userId == state.myId,
                    // The leader removes members or hands them the lead, never themselves.
                    onRemove = if (state.isLeader && member.role != PartyRole.LEADER) ({ onRemove(member) }) else null,
                    onTransfer = if (state.isLeader && member.role != PartyRole.LEADER) ({ onTransfer(member) }) else null,
                    enabled = !state.busy
                )
            }
            if (state.isLeader) {
                Text(
                    text = stringResource(R.string.darood_info_leader_leave_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.testTag(DaroodTestTags.INFO_LEAVE_HINT)
                )
                PixelButton(
                    text = stringResource(R.string.darood_info_delete),
                    onClick = onDelete,
                    variant = PixelButtonVariant.BLUE,
                    enabled = !state.busy,
                    modifier = Modifier.testTag(DaroodTestTags.INFO_DELETE)
                )
            } else {
                PixelButton(
                    text = stringResource(R.string.darood_info_leave),
                    onClick = onLeave,
                    variant = PixelButtonVariant.BLUE,
                    enabled = !state.busy,
                    modifier = Modifier.testTag(DaroodTestTags.INFO_LEAVE)
                )
            }
        }
    }

    state.confirmingRemoval?.let { member ->
        val name = displayName(member)
        PixelConfirmDialog(
            title = stringResource(R.string.darood_info_remove_title, name),
            message = stringResource(R.string.darood_info_remove_message),
            confirmText = stringResource(R.string.darood_info_remove),
            dismissText = stringResource(R.string.darood_cancel),
            onConfirm = onConfirmRemoval,
            onDismiss = onDismissDialog
        )
    }
    if (state.confirmingLeave) {
        PixelConfirmDialog(
            title = stringResource(R.string.darood_info_leave_title, state.party?.name.orEmpty()),
            message = stringResource(R.string.darood_info_leave_message),
            confirmText = stringResource(R.string.darood_info_leave),
            dismissText = stringResource(R.string.darood_cancel),
            onConfirm = onConfirmLeave,
            onDismiss = onDismissDialog
        )
    }
    state.confirmingTransfer?.let { member ->
        PixelConfirmDialog(
            title = stringResource(R.string.darood_info_transfer_title, displayName(member)),
            message = stringResource(R.string.darood_info_transfer_message),
            confirmText = stringResource(R.string.darood_info_make_leader),
            dismissText = stringResource(R.string.darood_cancel),
            onConfirm = onConfirmTransfer,
            onDismiss = onDismissDialog
        )
    }
    state.deleteConfirmation?.let { typed ->
        val partyName = state.party?.name.orEmpty()
        PixelDialog(
            onDismissRequest = onDismissDialog,
            title = stringResource(R.string.darood_info_delete_title, partyName),
            confirmButtonText = stringResource(R.string.darood_info_delete_confirm),
            onConfirm = onConfirmDelete,
            dismissButtonText = stringResource(R.string.darood_cancel),
            onDismiss = onDismissDialog
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.darood_info_delete_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTheme.colors.onSurface
                )
                // Deleting waits for the name, typed exactly: a slip of the finger can't delete a party.
                PixelTextField(
                    value = typed,
                    onValueChange = onDeleteConfirmationChange,
                    label = stringResource(R.string.darood_info_delete_field),
                    placeholder = partyName,
                    errorText = if (typed.isNotBlank() && !state.deleteConfirmed) stringResource(R.string.darood_info_delete_mismatch) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.INFO_DELETE_FIELD)
                )
            }
        }
    }
    state.newName?.let { name ->
        PixelDialog(
            onDismissRequest = onDismissDialog,
            title = stringResource(R.string.darood_info_rename_title),
            confirmButtonText = stringResource(R.string.darood_info_rename_save),
            onConfirm = onConfirmRename,
            dismissButtonText = stringResource(R.string.darood_cancel),
            onDismiss = onDismissDialog
        ) {
            PixelTextField(
                value = name,
                onValueChange = onNewNameChange,
                label = stringResource(R.string.darood_create_name),
                errorText = when (state.newNameProblem) {
                    com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_SHORT -> stringResource(R.string.darood_create_name_too_short)
                    com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_LONG -> stringResource(R.string.darood_create_name_too_long)
                    null -> null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.INFO_RENAME_FIELD)
            )
        }
    }
}

/** Why the last change didn't happen. */
@Composable
private fun ProblemLine(problem: InfoProblem) {
    Text(
        text = stringResource(
            when (problem) {
                InfoProblem.Offline -> R.string.darood_info_offline
                is InfoProblem.Refused -> when (problem.refusal) {
                    DaroodRefusal.NOT_LEADER -> R.string.darood_info_not_leader
                    DaroodRefusal.NOT_A_MEMBER -> R.string.darood_info_not_member
                    DaroodRefusal.LEADER_MUST_HAND_OVER -> R.string.darood_info_must_hand_over
                    else -> R.string.darood_info_failed
                }
            }
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = PixelTheme.colors.error,
        modifier = Modifier.testTag(DaroodTestTags.INFO_PROBLEM).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun displayName(member: PartyMember): String =
    member.username?.let { stringResource(R.string.darood_username_handle, it) } ?: stringResource(R.string.darood_someone)

/** One member: their username, their role, the day they joined, and for the leader, a way to hand over or remove. */
@Composable
private fun MemberRow(member: PartyMember, isMe: Boolean, onRemove: (() -> Unit)?, onTransfer: (() -> Unit)?, enabled: Boolean) {
    val colors = PixelTheme.colors
    val name = displayName(member)
    PixelCard(
        variant = PixelPanelVariant.BORDER,
        contentPadding = 12.dp,
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.member(member.userId))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isMe) stringResource(R.string.darood_info_you, name) else name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = inkOnPanel(colors.primaryText)
                    )
                    member.joinedAt?.let { joined ->
                        Text(
                            text = stringResource(R.string.darood_info_joined, joinedDate(joined)),
                            style = MaterialTheme.typography.bodySmall,
                            color = inkOnPanel(colors.onSurfaceVariant)
                        )
                    }
                }
                Text(
                    text = stringResource(if (member.role == PartyRole.LEADER) R.string.darood_badge_leader else R.string.darood_badge_member),
                    style = MaterialTheme.typography.labelMedium,
                    color = inkOnPanel(colors.primaryText)
                )
            }
            // The leader's tools for this member, on their own line so the name keeps its room.
            if (onTransfer != null || onRemove != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                    if (onTransfer != null) {
                        val label = stringResource(R.string.darood_info_make_leader_label, name)
                        PixelButton(
                            text = stringResource(R.string.darood_info_make_leader),
                            onClick = onTransfer,
                            variant = PixelButtonVariant.BLUE,
                            enabled = enabled,
                            modifier = Modifier
                                .semantics { contentDescription = label }
                                .testTag(DaroodTestTags.memberTransfer(member.userId))
                        )
                    }
                    if (onRemove != null) {
                        val label = stringResource(R.string.darood_info_remove_label, name)
                        PixelButton(
                            text = stringResource(R.string.darood_info_remove),
                            onClick = onRemove,
                            variant = PixelButtonVariant.BLUE,
                            enabled = enabled,
                            modifier = Modifier
                                .semantics { contentDescription = label }
                                .testTag(DaroodTestTags.memberRemove(member.userId))
                        )
                    }
                }
            }
        }
    }
}

/** The day, in the phone's own time zone and language: Oct 19, 2026. */
private fun joinedDate(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)
