package com.pixelquest.app.ui.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.PointsCalculator
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TaskPromptViewModel @Inject constructor(
    private val taskCompletionRepository: TaskCompletionRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    fun onTaskCompleted(taskId: Long, wasCompleted: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val log = TaskCompletionLogEntity(
                taskId = taskId,
                completedAt = LocalDateTime.now(),
                wasCompleted = wasCompleted
            )
            taskCompletionRepository.logTaskCompletion(log)
            if (wasCompleted) {
                val profile = userProfileRepository.getUserProfile().first()
                if (profile != null) {
                    val earnedXp = PointsCalculator.calculateXpForTask()
                    val updated = profile.copy(totalXp = profile.totalXp + earnedXp)
                    userProfileRepository.updateUserProfile(updated)
                }
            }
            onDone()
        }
    }
}
