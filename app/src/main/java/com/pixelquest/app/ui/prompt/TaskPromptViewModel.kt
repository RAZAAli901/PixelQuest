package com.pixelquest.app.ui.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.PointsCalculator
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskPromptViewModel @Inject constructor(
    private val taskCompletionRepository: TaskCompletionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _pointsAwardedTrigger = MutableStateFlow<Int?>(null)
    val pointsAwardedTrigger: StateFlow<Int?> = _pointsAwardedTrigger.asStateFlow()

    fun onTaskCompleted(taskId: Long, wasCompleted: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val streak = streakRepository.getCurrentStreak().first()
            val currentStreakCount = streak?.currentStreak ?: 0
            val earnedXp = if (wasCompleted) PointsCalculator.calculateXpForTask(currentStreak = currentStreakCount) else 0

            val log = TaskCompletionLogEntity(
                taskId = taskId,
                completedDate = LocalDate.now(),
                wasCompleted = wasCompleted,
                pointsAwarded = earnedXp
            )
            taskCompletionRepository.insertLog(log)
            if (wasCompleted) {
                val isSimpleMode = settingsRepository?.simpleModeEnabled?.first() ?: false
                if (!isSimpleMode) {
                    _pointsAwardedTrigger.value = earnedXp
                }
                val profile = userProfileRepository.getProfile().first()
                if (profile != null) {
                    val updated = profile.copy(totalXp = profile.totalXp + earnedXp)
                    userProfileRepository.updateProfile(updated)
                }
            }
            onDone()
        }
    }
}
