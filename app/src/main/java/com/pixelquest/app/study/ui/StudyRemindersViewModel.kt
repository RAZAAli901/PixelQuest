package com.pixelquest.app.study.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.study.data.ReminderSave
import com.pixelquest.app.study.data.StudyReminderRepository
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.domain.ReminderProblem
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.isOver
import com.pixelquest.app.study.domain.occurrencesIn
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.ui.navigation.TabSharing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the reminders part of a study party's screen shows. */
data class ReminderUiState(
    /** This week's reminders, soonest first, for the banner on This week. */
    val thisWeek: List<ReminderLine> = emptyList(),
    val form: ReminderForm? = null,
    /** The open reminder's delete, waiting for the player to confirm it. */
    val confirmingDelete: Boolean = false,
    val notice: ReminderNotice? = null,
    val today: java.time.LocalDate = java.time.LocalDate.MIN,
    val zone: java.time.ZoneId = java.time.ZoneOffset.UTC
)

/** A line about the last reminder saved. */
sealed interface ReminderNotice {
    data object SavedOffline : ReminderNotice
    data class Refused(val refusal: StudyRefusal) : ReminderNotice
}

/**
 * A study party's reminders on its screen (docs/study-reminders.md), beside [StudyPartyViewModel]
 * on the same navigation entry (`partyId`). Saving goes through [StudyReminderRepository], which
 * keeps it on the phone first, so it's never lost to a bad connection.
 */
@HiltViewModel
class StudyRemindersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminders: StudyReminderRepository,
    private val study: StudyRepository,
    parties: com.pixelquest.app.party.data.PartyRepository
) : ViewModel() {

    val partyId: String = savedStateHandle[StudyPartyViewModel.ARG_PARTY] ?: ""

    private val form = MutableStateFlow<ReminderForm?>(null)
    private val notice = MutableStateFlow<ReminderNotice?>(null)
    private val confirmingDelete = MutableStateFlow(false)

    val uiState: StateFlow<ReminderUiState> = combine(reminders.observeReminders(partyId), form, notice, confirmingDelete) { all, form, notice, deleting ->
        ReminderUiState(thisWeek = thisWeekOf(all), form = form, confirmingDelete = deleting && form?.editingId != null, notice = notice, today = study.today(), zone = study.zone())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, TabSharing, ReminderUiState(today = study.today(), zone = study.zone()))

    /**
     * The reminders with an occurrence in the player's week, Monday to Sunday: a week's all week, a
     * day's or a time's until it's past. Soonest first; weeks first.
     */
    private fun thisWeekOf(all: List<com.pixelquest.app.study.domain.StudyReminder>): List<ReminderLine> {
        val zone = study.zone()
        val now = study.now()
        val from = study.thisWeek().atStartOfDay(zone).toInstant()
        val to = study.thisWeek().plusWeeks(1).atStartOfDay(zone).toInstant()
        val me = study.myId()
        return all.flatMap { reminder ->
            reminder.occurrencesIn(from, to)
                .filter { reminder.scope == ReminderScope.WEEK || !it.isOver(now) }
                .take(1)
                .map { ReminderLine(reminder, it, mine = me != null && reminder.createdBy == me) }
        }.sortedWith(compareBy<ReminderLine> { it.reminder.scope != ReminderScope.WEEK }.thenBy { it.occurrence.start.toInstant() })
    }

    init {
        // Fetched once the party is on the phone (a first visit brings it): its reminders go with it.
        viewModelScope.launch {
            parties.observeParty(partyId).filterNotNull().first()
            reminders.refresh(partyId)
        }
    }

    /** The player pulled to refresh: the party's reminders too. */
    fun refresh() {
        viewModelScope.launch { reminders.refresh(partyId) }
    }

    fun openCreate() {
        form.value = ReminderForm.new(study.today())
    }

    /** The Edit sheet for [reminder], as it is. Who may change it is the screen's to know, and the server's to check. */
    fun openEdit(reminder: com.pixelquest.app.study.domain.StudyReminder) {
        form.value = ReminderForm.of(reminder, study.today(), study.zone(), study.myId())
    }

    fun change(next: ReminderForm) {
        form.value = next
    }

    fun close() {
        form.value = null
        confirmingDelete.value = false
    }

    fun askDelete() {
        if (form.value?.editingId != null) confirmingDelete.value = true
    }

    fun cancelDelete() {
        confirmingDelete.value = false
    }

    /** Deletes the open reminder: at once on the phone, then on the server (or once there's a connection). */
    fun confirmDelete() {
        val id = form.value?.editingId ?: return
        confirmingDelete.value = false
        viewModelScope.launch {
            val reminder = reminders.observeReminders(partyId).first().firstOrNull { it.id == id }
            close()
            when (val result = reminder?.let { reminders.delete(it) }) {
                ReminderSave.Waiting -> notice.value = ReminderNotice.SavedOffline
                is ReminderSave.Refused -> notice.value = ReminderNotice.Refused(result.refusal)
                else -> Unit
            }
        }
    }

    fun dismissNotice() {
        notice.value = null
    }

    fun save() {
        val current = form.value ?: return
        if (current.saving) return
        val zone = study.zone()
        if (current.problem(zone) != null) {
            form.value = current.copy(triedToSave = true)
            return
        }
        form.value = current.copy(saving = true)
        viewModelScope.launch {
            val draft = current.draft(zone)
            val editing = current.editingId?.let { id -> reminders.observeReminders(partyId).first().firstOrNull { it.id == id } }
            val result = if (editing != null) reminders.edit(editing, draft) else reminders.create(partyId, draft)
            when (result) {
                ReminderSave.Sent -> close()
                ReminderSave.Waiting -> {
                    close()
                    notice.value = ReminderNotice.SavedOffline
                }
                is ReminderSave.Invalid -> form.value = current.copy(triedToSave = true)
                is ReminderSave.Refused -> {
                    close()
                    notice.value = ReminderNotice.Refused(result.refusal)
                }
            }
        }
    }
}

/** What a [ReminderProblem] says. */
fun reminderProblemText(problem: ReminderProblem): Int = when (problem) {
    ReminderProblem.TITLE -> com.pixelquest.app.R.string.study_reminder_problem_title
    ReminderProblem.WHEN -> com.pixelquest.app.R.string.study_reminder_problem_when
    ReminderProblem.REPEAT -> com.pixelquest.app.R.string.study_reminder_problem_repeat
    ReminderProblem.ALERTS -> com.pixelquest.app.R.string.study_reminder_problem_alerts
    ReminderProblem.TIME_ZONE -> com.pixelquest.app.R.string.study_reminder_problem_zone
}
