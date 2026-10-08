package com.pixelquest.app.ui.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskPromptViewModel @Inject constructor(
    private val taskResultRecorder: TaskResultRecorder,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    val isSimpleMode: StateFlow<Boolean> = (settingsRepository?.simpleModeEnabled ?: flowOf(false))
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** [occurrenceDate]: the day of the quest the prompt is for (see ReminderAnswer). */
    fun onTaskCompleted(taskId: Long, wasCompleted: Boolean, occurrenceDate: LocalDate? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            // The prompt closes as soon as an answer is tapped, which cancels viewModelScope; the
            // answer must still be saved.
            withContext(NonCancellable) { record(taskId, wasCompleted, occurrenceDate) }
            onDone()
        }
    }

    private suspend fun record(taskId: Long, wasCompleted: Boolean, occurrenceDate: LocalDate?) {
        // "Not yet" records nothing, so the task can still be completed later today.
        val day = com.pixelquest.app.domain.ReminderAnswer.dateToRecord(occurrenceDate, LocalDate.now())
        if (wasCompleted && day != null) {
            taskResultRecorder.recordCompleted(taskId, day)
        }
        taskAlarmScheduler.clearReminder(taskId)
    }
}
