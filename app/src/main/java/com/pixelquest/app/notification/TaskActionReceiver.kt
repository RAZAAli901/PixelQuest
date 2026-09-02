package com.pixelquest.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.PointsCalculator
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskCompletionRepository: TaskCompletionRepository

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var streakRepository: StreakRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val wasCompleted = intent.getBooleanExtra("EXTRA_WAS_COMPLETED", false)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val streak = streakRepository.getCurrentStreak().first()
                val currentStreakCount = streak?.currentStreak ?: 0
                val earnedXp = if (wasCompleted) PointsCalculator.calculateXpForTask(currentStreak = currentStreakCount) else 0

                val completionLog = TaskCompletionLogEntity(
                    taskId = taskId,
                    completedDate = LocalDate.now(),
                    wasCompleted = wasCompleted,
                    pointsAwarded = earnedXp
                )
                taskCompletionRepository.insertLog(completionLog)
                if (wasCompleted) {
                    val profile = userProfileRepository.getProfile().first()
                    if (profile != null) {
                        val updatedXp = profile.totalXp + earnedXp
                        userProfileRepository.updateProfile(profile.copy(totalXp = updatedXp))
                    }
                }
            } finally {
                NotificationManagerCompat.from(context).cancel(taskId.toInt())
                pendingResult.finish()
            }
        }
    }
}
