package com.pixelquest.app.study.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.domain.PartyTab
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.study.domain.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A study party's own screen, as navigation opens it (`partyId`): its party and members from the
 * party core, its items, ticks and board from the study cache, refreshed from the server on opening.
 * Every action saves or asks the server through [StudyRepository], which keeps the rules.
 */
@HiltViewModel
class StudyPartyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parties: PartyRepository,
    private val study: StudyRepository,
    settings: SettingsRepository? = null
) : ViewModel() {

    val partyId: String = savedStateHandle[ARG_PARTY] ?: ""

    private val tab = MutableStateFlow(PartyTab.THIS_WEEK)
    private val refreshing = MutableStateFlow(false)
    private val loaded = MutableStateFlow(false)
    private val notice = MutableStateFlow<StudyNotice?>(null)
    private val form = MutableStateFlow<ItemForm?>(null)
    private val confirmingDelete = MutableStateFlow<String?>(null)

    private val weekStart = study.thisWeek()

    private val partyAndMembers = combine(parties.observeParty(partyId), parties.observeMembers(partyId)) { party, members -> party to members }

    private val cards = combine(partyAndMembers, study.observeItems(partyId), study.observeCompletions(partyId)) { (party, members), items, completions ->
        val me = study.myId()
        val isLeader = party?.leaderId != null && party.leaderId == me
        Triple(party, members, itemRowsOf(items, completions, members, me, isLeader, study.now(), study.zone()))
    }

    private val scores = combine(
        study.observeScores(partyId, weekStart),
        study.observeAllScores(partyId),
        study.observeWinners(partyId)
    ) { week, all, winners -> Triple(week, all.filter { it.weekStart.isBefore(weekStart) }, winners) }

    private val flags = combine(tab, refreshing, loaded, combine(notice, form, confirmingDelete, ::Triple), settings?.simpleModeEnabled ?: flowOf(false)) { tab, refreshing, loaded, (notice, form, deleting), simple ->
        Flags(tab, refreshing, loaded, notice, simple, form, deleting)
    }

    private data class Flags(val tab: PartyTab, val refreshing: Boolean, val loaded: Boolean, val notice: StudyNotice?, val simple: Boolean, val form: ItemForm?, val deleting: String?)

    val uiState: StateFlow<StudyPartyUiState> = combine(cards, scores, study.observePendingCount(), flags) { (party, members, rows), (weekScores, past, winners), waiting, flags ->
        val me = study.myId()
        val week = thisWeekOf(rows, weekStart, study.zone())
        StudyPartyUiState(
            party = party,
            myId = me,
            isLeader = party?.leaderId != null && party.leaderId == me,
            tab = flags.tab,
            members = members,
            items = rows,
            thisWeek = week,
            weekSections = weekSectionsOf(week, study.zone()),
            weekProgress = weekProgressOf(week),
            pinned = rows.filter { it.item.pinned },
            weekStart = weekStart,
            today = study.today(),
            board = boardOf(weekScores, members, me),
            myScore = weekScores.firstOrNull { it.userId == me },
            pastScores = past,
            winners = winners,
            refused = rows.mapNotNull { row -> row.mine?.takeIf { it.state == SyncState.FAILED } },
            waiting = waiting,
            refreshing = flags.refreshing,
            loaded = flags.loaded,
            simpleMode = flags.simple,
            notice = flags.notice,
            form = flags.form,
            confirmingDelete = flags.deleting?.let { id -> rows.firstOrNull { it.item.id == id } }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyPartyUiState(weekStart = weekStart))

    init {
        refresh()
    }

    fun selectTab(selected: PartyTab) {
        tab.value = selected
    }

    /** Brings the party and its study data from the server. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                parties.refreshParty(partyId)
                report(study.refresh(partyId))
            } finally {
                refreshing.value = false
                loaded.value = true
            }
        }
    }

    /** Ticks an item off for the player: saved at once, sent when it can be. */
    fun complete(itemId: String) {
        val row = uiState.value.items.firstOrNull { it.item.id == itemId } ?: return
        if (row.done) return
        viewModelScope.launch {
            val done = study.complete(row.item)
            if (done.state == SyncState.PENDING) notice.value = StudyNotice.Offline
        }
    }

    /** Takes the player's tick back, as the rules allow. */
    fun undo(itemId: String) {
        viewModelScope.launch { report(study.undo(itemId)) }
    }

    /** The leader pins or unpins an item. */
    fun togglePin(itemId: String) {
        val row = uiState.value.items.firstOrNull { it.item.id == itemId } ?: return
        viewModelScope.launch { report(study.setPinned(itemId, !row.item.pinned)) }
    }

    /** Its creator or the leader deletes an item. */
    fun delete(itemId: String) {
        viewModelScope.launch { report(study.deleteItem(itemId)) }
    }

    /** Logs study time for today. */
    fun logStudy(minutes: Int) {
        viewModelScope.launch {
            val session = study.logStudy(partyId, minutes)
            if (session.state == SyncState.PENDING) notice.value = StudyNotice.Offline
        }
    }

    /** Tries a refused tick again, or drops it. */
    fun retry(clientId: String) {
        viewModelScope.launch { study.retry(clientId) }
    }

    fun discard(clientId: String) {
        viewModelScope.launch { study.discard(clientId) }
    }

    /** Opens the Add item sheet, empty. */
    fun openAddItem() {
        form.value = ItemForm()
    }

    /** The sheet's fields, as the player changes them. */
    fun updateForm(changed: ItemForm) {
        if (form.value != null) form.value = changed
    }

    fun closeForm() {
        form.value = null
    }

    /**
     * Saves the sheet: a new item, or the changes to one. With a [ItemForm.problem] it stays open
     * and shows it; the server's refusal keeps it open too, saying why.
     */
    fun saveForm() {
        val current = form.value ?: return
        if (current.saving) return
        if (current.problem != null) {
            form.value = current.copy(triedToSave = true)
            return
        }
        form.value = current.copy(saving = true, triedToSave = true)
        viewModelScope.launch {
            val draft = current.draft(study.zone())
            val isLeader = uiState.value.isLeader
            val result = when (val id = current.editingId) {
                null -> study.addItem(partyId, draft, pinned = current.pinned && isLeader)
                else -> {
                    val wasPinned = uiState.value.items.firstOrNull { it.item.id == id }?.item?.pinned ?: false
                    val edited = study.editItem(id, draft)
                    // The pin is the leader's alone, and a call of its own.
                    if (edited is SupabaseResult.Success && isLeader && current.pinned != wasPinned) study.setPinned(id, current.pinned) else edited
                }
            }
            report(result)
            form.value = if (result is SupabaseResult.Success) null else form.value?.copy(saving = false)
        }
    }

    /** Opens the Edit item sheet with [itemId]'s fields, for its creator or the leader. */
    fun editItem(itemId: String) {
        val row = uiState.value.items.firstOrNull { it.item.id == itemId } ?: return
        if (!row.canEdit) return
        form.value = ItemForm.of(row.item, study.zone())
    }

    /** Asks the player to be sure before [itemId] goes, with every tick on it. */
    fun askDelete(itemId: String) {
        if (uiState.value.items.any { it.item.id == itemId && it.canEdit }) confirmingDelete.value = itemId
    }

    fun cancelDelete() {
        confirmingDelete.value = null
    }

    /** They're sure: the item goes, for everyone. */
    fun confirmDelete() {
        val id = confirmingDelete.value ?: return
        confirmingDelete.value = null
        delete(id)
    }

    fun dismissNotice() {
        notice.value = null
    }

    private fun report(result: SupabaseResult<*>) {
        notice.value = when {
            result is SupabaseResult.Success -> null
            result is SupabaseResult.NetworkError -> StudyNotice.Offline
            else -> StudyRefusal.of(result)?.let { StudyNotice.Refused(it) }
        }
    }

    companion object {
        const val ARG_PARTY = "partyId"
    }
}
