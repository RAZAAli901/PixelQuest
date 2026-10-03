package com.pixelquest.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.MainActivity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

@HiltWorker
class MissedTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val settingsRepository: SettingsRepository,
    private val taskResultRecorder: TaskResultRecorder
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tasks = taskRepository.getTasksForDay(today).first()
        val logs = taskCompletionRepository.getLogsForDate(today).first()
        val loggedTaskIds = logs.map { it.taskId }.toSet()

        val overdueTasks = tasks.filter { task ->
            if (loggedTaskIds.contains(task.id)) return@filter false
            val scheduledDateTime = LocalDateTime.of(today, task.scheduledTime)
            val cutoffTime = scheduledDateTime.plusHours(2)
            now.isAfter(cutoffTime)
        }

        // Recorded only if still without a result: a task completed a moment ago is left alone.
        val missedTasks = overdueTasks.filter { task -> taskResultRecorder.recordNotDone(task.id, today) }

        if (missedTasks.isNotEmpty() && settingsRepository.isNotificationsEnabled.first()) {
            notifyMissed(missedTasks)
        }

        return Result.success()
    }

    private suspend fun notifyMissed(missedTasks: List<TaskEntity>) {
        val isSimpleMode = settingsRepository.simpleModeEnabled.first()
        val soundEnabled = settingsRepository.isNotificationSoundEnabled.first()
        val vibrationEnabled = settingsRepository.isNotificationVibrationEnabled.first()
        val openApp = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = NotificationManagerCompat.from(applicationContext)
        missedTasks.forEach { task ->
            // Replace the task's reminder (same id) so the shade doesn't show both.
            manager.cancel(task.id.toInt())
            val notification = NotificationHelper.buildMissedTaskNotification(
                context = applicationContext,
                taskId = task.id,
                taskName = task.name,
                contentIntent = openApp,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                isSimpleMode = isSimpleMode
            )
            try {
                manager.notify(missedNotificationId(task.id), notification)
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS not granted; the missed log is still recorded.
            }
        }
    }

    companion object {
        fun missedNotificationId(taskId: Long): Int = (taskId * 10 + 3).toInt()
    }
}
