package com.pixelquest.app.study.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.study.data.ReminderSave
import com.pixelquest.app.study.data.StudyReminderRepository
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.domain.ReminderProblem
import com.pixelquest.app.study.domain.StudyRefusal
import com.pixelquest.app.ui.navigation.TabSharing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the reminders part of a study party's screen shows. */
data class ReminderUiState(
    val form: ReminderForm? = null,
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
    private val study: StudyRepository
) : ViewModel() {

    val partyId: String = savedStateHandle[StudyPartyViewModel.ARG_PARTY] ?: ""

    private val form = MutableStateFlow<ReminderForm?>(null)
    private val notice = MutableStateFlow<ReminderNotice?>(null)

    val uiState: StateFlow<ReminderUiState> = combine(form, notice) { form, notice ->
        ReminderUiState(form = form, notice = notice, today = study.today(), zone = study.zone())
    }.stateIn(viewModelScope, TabSharing, ReminderUiState(today = study.today(), zone = study.zone()))

    fun openCreate() {
        form.value = ReminderForm.new(study.today())
    }

    fun change(next: ReminderForm) {
        form.value = next
    }

    fun close() {
        form.value = null
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
