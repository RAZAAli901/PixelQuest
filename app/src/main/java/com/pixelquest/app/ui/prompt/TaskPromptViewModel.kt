package com.pixelquest.app.ui.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskPromptViewModel @Inject constructor(
    private val taskResultRecorder: TaskResultRecorder,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    val isSimpleMode: StateFlow<Boolean> = (settingsRepository?.simpleModeEnabled ?: flowOf(false))
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _pointsAwardedTrigger = MutableStateFlow<Int?>(null)
    val pointsAwardedTrigger: StateFlow<Int?> = _pointsAwardedTrigger.asStateFlow()

    fun onTaskCompleted(taskId: Long, wasCompleted: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (wasCompleted) {
                // 0 when the task was already completed today; no XP pop-up then.
                val earnedXp = taskResultRecorder.recordCompleted(taskId, LocalDate.now())
                val isSimpleMode = settingsRepository?.simpleModeEnabled?.first() ?: false
                if (earnedXp > 0 && !isSimpleMode) {
                    _pointsAwardedTrigger.value = earnedXp
                }
            } else {
                taskResultRecorder.recordNotDone(taskId, LocalDate.now())
            }
            onDone()
        }
    }
}
