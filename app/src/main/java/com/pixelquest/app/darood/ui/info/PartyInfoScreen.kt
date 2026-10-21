package com.pixelquest.app.darood.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.domain.DaroodRefusal
import com.pixelquest.app.darood.domain.DaroodRules
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.PartyAvatar
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.party.PartyKinds
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.Party
import com.pixelquest.app.party.domain.PartyMember
import com.pixelquest.app.party.domain.PartyRole
import com.pixelquest.app.party.ui.components.MemberList
import com.pixelquest.app.party.ui.components.displayName
import com.pixelquest.app.party.ui.shortName
import com.pixelquest.app.ui.components.PqConfirmDialog
import com.pixelquest.app.ui.components.PqDialog
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqDangerButton
import androidx.compose.material.icons.rounded.Logout
import com.pixelquest.app.ui.components.PqGroupLabel
import com.pixelquest.app.ui.components.PqSwitch
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.pqShape
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val newMilestone: String? = null,
    val busy: Boolean = false,
    val problem: InfoProblem? = null,
    val left: Boolean = false
) {
    val isLeader: Boolean get() = party?.leaderId != null && party.leaderId == myId

    /** The milestone typed, if it's above the party's total, as the server insists; null otherwise. */
    val newMilestoneValue: Long?
        get() {
            val typed = newMilestone?.toLongOrNull() ?: return null
            val total = party?.total ?: return null
            return typed.takeIf { it > total }
        }

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
    private val weekMutes: com.pixelquest.app.darood.sync.WeekRankingMutes? = null,
    account: DaroodAccount? = null
) : ViewModel() {

    val partyId: String = savedStateHandle["partyId"] ?: ""

    /** The party's goals, oldest first (docs/darood-weekly.md, History): read again as its goal changes. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val goals: StateFlow<List<com.pixelquest.app.darood.domain.PartyGoal>> = repository.observeParty(partyId)
        .map { it?.milestoneSetAt to it?.completedAt }
        .distinctUntilChanged()
        .mapLatest { (repository.fetchGoals(partyId) as? SupabaseResult.Success)?.data.orEmpty() }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    /** This party's weekly ranking notification is on (the Settings switch turns them all off). */
    val weekNotify: StateFlow<Boolean> = weekMutes?.muted?.let { muted ->
        kotlinx.coroutines.flow.combine(muted, weekMutes.allOn) { set, all -> all && partyId !in set }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, !weekMutes.silent(partyId))
    } ?: MutableStateFlow(true)

    fun setWeekNotify(on: Boolean) {
        weekMutes?.setMuted(partyId, !on)
        // Turning one party back on while all are off turns the weekly notifications back on.
        if (on && weekMutes?.allOn?.value == false) weekMutes.setAllOn(true)
    }

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

    fun dismissDialog() = _uiState.update { it.copy(confirmingRemoval = null, confirmingLeave = false, newName = null, confirmingTransfer = null, deleteConfirmation = null, newMilestone = null) }

    /** The leader puts the party on the race board, or takes it off. */
    fun setRaceBoard(on: Boolean) {
        if (!_uiState.value.isLeader || _uiState.value.busy) return
        _uiState.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            val result = repository.setRaceBoard(partyId, on)
            _uiState.update { it.copy(busy = false, problem = problemOf(result)) }
        }
    }

    /** The leader opens the milestone dialog, with the milestone as it stands (if any) to change. */
    fun startMilestone() {
        val party = _uiState.value.party ?: return
        if (!_uiState.value.isLeader || party.isFinished) return
        _uiState.update { it.copy(newMilestone = party.milestone?.toString() ?: "", problem = null) }
    }

    fun onMilestoneChange(raw: String) =
        _uiState.update { if (it.newMilestone == null) it else it.copy(newMilestone = milestoneDigits(raw)) }

    /** Saves the milestone typed, once it's above the party's total. */
    fun confirmMilestone() {
        val state = _uiState.value
        val target = state.newMilestoneValue ?: return
        if (state.busy) return
        _uiState.update { it.copy(newMilestone = null, busy = true) }
        viewModelScope.launch {
            val result = repository.setMilestone(partyId, target)
            _uiState.update { it.copy(busy = false, problem = problemOf(result)) }
        }
    }

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
    val weekNotify by viewModel.weekNotify.collectAsState()
    val goals by viewModel.goals.collectAsState()
    LaunchedEffect(state.left) { if (state.left) onLeft() }
    PartyInfoContent(
        state = state,
        weekNotify = weekNotify,
        onWeekNotify = viewModel::setWeekNotify,
        goals = goals,
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
        onMilestone = viewModel::startMilestone,
        onRaceBoard = viewModel::setRaceBoard,
        onMilestoneChange = viewModel::onMilestoneChange,
        onConfirmMilestone = viewModel::confirmMilestone,
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
    onMilestone: () -> Unit = {},
    onRaceBoard: (Boolean) -> Unit = {},
    onMilestoneChange: (String) -> Unit = {},
    onConfirmMilestone: () -> Unit = {},
    onDeleteConfirmationChange: (String) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    weekNotify: Boolean? = null,
    onWeekNotify: (Boolean) -> Unit = {},
    goals: List<com.pixelquest.app.darood.domain.PartyGoal> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp)
            .testTag(DaroodTestTags.INFO_SCREEN),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PqTopBar(stringResource(R.string.darood_party_info), onBack = onNavigateBack)
        val party = state.party ?: return@Column
        // What the party's kind has of its own: a Darood Party's milestone and race board.
        val kind = PartyKinds.of(party.type)
        PqCard(shadow = PqDimens.ShadowLg) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    PartyAvatar(party.name, size = 52.dp)
                    Column {
                        Text(text = party.name.uppercase(), style = PqType.Title, modifier = Modifier.semantics { heading() })
                        Text(
                            text = stringResource(
                                R.string.pq_party_card_line,
                                stringResource(party.type.shortName()),
                                pluralStringResource(R.plurals.darood_info_size, state.members.size, state.members.size, party.maxMembers)
                            ),
                            style = PqType.Meta
                        )
                    }
                }
                if (kind.rules.milestone) {
                    Box(Modifier.fillMaxWidth().height(PqDimens.BorderThin).background(PqColors.Ink))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp).heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = PqColors.Ink)
                        Text(
                            text = party.milestone?.let { stringResource(R.string.darood_info_milestone, formatCount(it)) }
                                ?: stringResource(R.string.darood_info_no_milestone),
                            style = PqType.BodyStrong,
                            modifier = Modifier.weight(1f).testTag(DaroodTestTags.INFO_MILESTONE)
                        )
                        // A finish is final: the milestone stays as it was reached.
                        if (state.isLeader && !party.isFinished) {
                            QuietAction(
                                text = stringResource(if (party.milestone == null) R.string.darood_info_set_milestone else R.string.darood_info_change_milestone),
                                enabled = !state.busy,
                                onClick = onMilestone,
                                modifier = Modifier.testTag(DaroodTestTags.INFO_SET_MILESTONE)
                            )
                        }
                    }
                }
            }
        }
        if (state.isLeader) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!party.isFinished) {
                    PqButton(
                        text = stringResource(R.string.darood_add_title),
                        onClick = onAddPeople,
                        icon = Icons.Rounded.PersonAdd,
                        modifier = Modifier.weight(1.3f).testTag(DaroodTestTags.INFO_ADD_PEOPLE)
                    )
                }
                PqButton(
                    text = stringResource(R.string.darood_info_rename),
                    onClick = onRename,
                    style = PqButtonStyle.Secondary,
                    icon = Icons.Rounded.Edit,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f).testTag(DaroodTestTags.INFO_RENAME)
                )
            }
        }
        if (state.isLeader && kind.rules.raceBoard) {
            RaceBoardSwitch(on = party.isPublicRace, hasMilestone = party.milestone != null, enabled = !state.busy, onChange = onRaceBoard)
        } else if (party.isPublicRace && kind.rules.raceBoard) {
            Text(
                text = stringResource(R.string.darood_race_on_board),
                style = PqType.Meta,
                modifier = Modifier.testTag(DaroodTestTags.INFO_RACE_NOTE)
            )
        }
        if (goals.isNotEmpty() && kind.rules.raceBoard) GoalHistory(goals, party.total)
        if (weekNotify != null && kind.rules.raceBoard && state.members.any { it.userId == state.myId && it.status == com.pixelquest.app.party.domain.MemberStatus.ACTIVE }) {
            WeekNotifySwitch(on = weekNotify, onChange = onWeekNotify)
        }
        PqGroupLabel(
            stringResource(R.string.darood_info_members) + " · " + state.members.size,
            Modifier.semantics { heading() }
        )
        state.problem?.let { ProblemLine(it) }
        MemberList(
            members = state.members,
            myId = state.myId,
            isLeader = state.isLeader,
            enabled = !state.busy,
            onRemove = onRemove,
            onTransfer = onTransfer
        )
        if (state.isLeader) {
            Text(
                text = stringResource(R.string.darood_info_leader_leave_hint),
                style = PqType.Meta,
                modifier = Modifier.testTag(DaroodTestTags.INFO_LEAVE_HINT)
            )
            PqDangerButton(stringResource(R.string.darood_info_delete), onDelete, Modifier.testTag(DaroodTestTags.INFO_DELETE), enabled = !state.busy)
        } else {
            PqDangerButton(stringResource(R.string.darood_info_leave), onLeave, Modifier.testTag(DaroodTestTags.INFO_LEAVE), enabled = !state.busy, icon = Icons.Rounded.Logout)
        }
    }

    state.confirmingRemoval?.let { member ->
        val name = displayName(member)
        PqConfirmDialog(
            title = stringResource(R.string.darood_info_remove_title, name),
            message = stringResource(R.string.darood_info_remove_message),
            confirm = stringResource(R.string.darood_info_remove),
            dismiss = stringResource(R.string.darood_cancel),
            onConfirm = onConfirmRemoval,
            onDismiss = onDismissDialog
        )
    }
    if (state.confirmingLeave) {
        PqConfirmDialog(
            title = stringResource(R.string.darood_info_leave_title, state.party?.name.orEmpty()),
            message = stringResource(R.string.darood_info_leave_message),
            confirm = stringResource(R.string.darood_info_leave),
            dismiss = stringResource(R.string.darood_cancel),
            onConfirm = onConfirmLeave,
            onDismiss = onDismissDialog
        )
    }
    state.confirmingTransfer?.let { member ->
        PqConfirmDialog(
            title = stringResource(R.string.darood_info_transfer_title, displayName(member)),
            message = stringResource(state.party?.let { PartyKinds.of(it.type).handOverRes } ?: R.string.darood_info_transfer_message),
            confirm = stringResource(R.string.darood_info_make_leader),
            dismiss = stringResource(R.string.darood_cancel),
            onConfirm = onConfirmTransfer,
            onDismiss = onDismissDialog
        )
    }
    state.deleteConfirmation?.let { typed ->
        val partyName = state.party?.name.orEmpty()
        PqDialog(
            onDismissRequest = onDismissDialog,
            title = stringResource(R.string.darood_info_delete_title, partyName),
            confirm = stringResource(R.string.darood_info_delete_confirm),
            onConfirm = onConfirmDelete,
            dismiss = stringResource(R.string.darood_cancel),
            onDismiss = onDismissDialog
        ) {
            DeleteConfirmation(typed = typed, partyName = partyName, confirmed = state.deleteConfirmed, onTypedChange = onDeleteConfirmationChange)
        }
    }
    state.newMilestone?.let { typed ->
        PqDialog(
            onDismissRequest = onDismissDialog,
            title = stringResource(R.string.darood_info_milestone_title),
            confirm = stringResource(R.string.darood_info_rename_save),
            onConfirm = onConfirmMilestone,
            dismiss = stringResource(R.string.darood_cancel),
            onDismiss = onDismissDialog
        ) {
            MilestoneField(typed = typed, total = state.party?.total ?: 0, onTypedChange = onMilestoneChange)
        }
    }
    state.newName?.let { name ->
        PqDialog(
            onDismissRequest = onDismissDialog,
            title = stringResource(R.string.darood_info_rename_title),
            confirm = stringResource(R.string.darood_info_rename_save),
            onConfirm = onConfirmRename,
            dismiss = stringResource(R.string.darood_cancel),
            onDismiss = onDismissDialog
        ) {
            RenameField(name = name, problem = state.newNameProblem, onNameChange = onNewNameChange)
        }
    }
}

/**
 * The leader's switch for the race board, with what joining shares said plainly: the party's name,
 * how many are in it, its total, how far it is to its milestone and when it finished. Never anyone's
 * name or their numbers. The whole row is the switch, read by TalkBack as one.
 */
@Composable
internal fun RaceBoardSwitch(on: Boolean, hasMilestone: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DaroodTestTags.INFO_RACE_SWITCH)
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .toggleable(value = on, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .heightIn(min = 48.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.darood_race_join), style = PqType.BodyStrong)
            Text(text = stringResource(R.string.darood_race_explained), style = PqType.Meta.copy(color = PqColors.Ink2))
            if (!hasMilestone) {
                Text(text = stringResource(R.string.darood_race_needs_milestone), style = PqType.Meta)
            }
        }
        // The row handles the toggle; the switch only shows it.
        PqSwitch(checked = on, onCheckedChange = null)
    }
}

/** The party's goals, in order: "Goal 1 reached on 21 Oct", "Goal 2 in progress: 1,200 of 2,000". */
@Composable
internal fun GoalHistory(goals: List<com.pixelquest.app.darood.domain.PartyGoal>, total: Long) {
    val dates = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.INFO_GOALS)) {
        PqGroupLabel(stringResource(R.string.darood_goal_history), Modifier.semantics { heading() })
        goals.sortedBy { it.sequence }.forEach { goal ->
            val reachedAt = goal.reachedAt
            Text(
                text = if (reachedAt != null) {
                    stringResource(R.string.darood_goal_history_reached, goal.sequence, formatCount(goal.target), reachedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dates))
                } else {
                    stringResource(R.string.darood_goal_history_open, goal.sequence, formatCount(minOf(total, goal.target)), formatCount(goal.target))
                },
                style = if (reachedAt != null) PqType.Body else PqType.BodyStrong,
                modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.infoGoal(goal.sequence))
            )
        }
    }
}

/** The player's switch for this party's weekly ranking notification, on this phone. The whole row toggles. */
@Composable
internal fun WeekNotifySwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(DaroodTestTags.INFO_WEEK_SWITCH)
            .comic(radius = PqDimens.Radius, border = PqDimens.BorderThin, shadow = 0.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .heightIn(min = 48.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.darood_week_switch), style = PqType.BodyStrong)
            Text(text = stringResource(R.string.darood_week_switch_party), style = PqType.Meta.copy(color = PqColors.Ink2))
        }
        PqSwitch(checked = on, onCheckedChange = null)
    }
}

/** A small action in the primary text colour, as v2's quiet buttons are, that can wait while busy. */
@Composable
private fun QuietAction(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PqType.MetaStrong.copy(color = PqColors.RedText, fontSize = 13.bsp),
        modifier = modifier
            .clip(pqShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    )
}

/**
 * The milestone dialog's field: digits only, and why the number won't do while it's not above the
 * party's [total] (the server insists on that too).
 */
@Composable
internal fun MilestoneField(typed: String, total: Long, onTypedChange: (String) -> Unit) {
    val tooLow = typed.isNotEmpty() && (typed.toLongOrNull() ?: 0) <= total
    PixelTextField(
        value = typed,
        onValueChange = onTypedChange,
        label = stringResource(R.string.darood_info_milestone_field),
        placeholder = stringResource(R.string.darood_create_milestone_placeholder),
        errorText = if (tooLow) stringResource(R.string.darood_info_milestone_too_low, formatCount(total)) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.INFO_MILESTONE_FIELD)
    )
}

/**
 * What the milestone field keeps of what was typed or pasted: digits only (any script's, as 0-9),
 * no leading zeros, at most as many as the create form allows.
 */
fun milestoneDigits(raw: String): String {
    val digits = buildString { raw.forEach { if (it.isDigit()) append(it.digitToInt()) } }
    if (digits.isEmpty()) return ""
    return digits.trimStart('0').ifEmpty { "0" }.take(com.pixelquest.app.darood.ui.create.CreatePartyViewModel.MILESTONE_DIGITS)
}

/** The rename dialog's field: the name, and why it won't do, if it won't. */
@Composable
internal fun RenameField(name: String, problem: com.pixelquest.app.darood.ui.create.PartyNameProblem?, onNameChange: (String) -> Unit) {
    PixelTextField(
        value = name,
        onValueChange = onNameChange,
        label = stringResource(R.string.darood_create_name),
        errorText = when (problem) {
            com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_SHORT -> stringResource(R.string.darood_create_name_too_short)
            com.pixelquest.app.darood.ui.create.PartyNameProblem.TOO_LONG -> stringResource(R.string.darood_create_name_too_long)
            null -> null
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.INFO_RENAME_FIELD)
    )
}

/** The delete dialog's body: what deleting does, and the party's name to type. */
@Composable
internal fun DeleteConfirmation(typed: String, partyName: String, confirmed: Boolean, onTypedChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.darood_info_delete_message), style = PqType.Body.copy(color = PqColors.Ink2))
        // Deleting waits for the name, typed exactly: a slip of the finger can't delete a party.
        PixelTextField(
            value = typed,
            onValueChange = onTypedChange,
            label = stringResource(R.string.darood_info_delete_field),
            placeholder = partyName,
            errorText = if (typed.isNotBlank() && !confirmed) stringResource(R.string.darood_info_delete_mismatch) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrect = false, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag(DaroodTestTags.INFO_DELETE_FIELD)
        )
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
                    DaroodRefusal.MILESTONE_TOO_LOW -> R.string.darood_info_refused_too_low
                    DaroodRefusal.PARTY_CLOSED -> R.string.darood_info_refused_finished
                    else -> R.string.darood_info_failed
                }
            }
        ),
        style = PqType.Body.copy(color = PqColors.Error),
        modifier = Modifier.testTag(DaroodTestTags.INFO_PROBLEM).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/** The day, in the phone's own time zone and language: Oct 19, 2026. */
