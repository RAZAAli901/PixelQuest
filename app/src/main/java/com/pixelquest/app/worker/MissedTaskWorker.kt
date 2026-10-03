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
        val today = now.toLocalDate()

        // Yesterday too: a quest at 22:00 or later only passes its cutoff after midnight. Those are
        // recorded quietly; a notification in the night about yesterday would help nobody.
        recordMissed(today.minusDays(1), now)
        val missedTasks = recordMissed(today, now)

        if (missedTasks.isNotEmpty() && settingsRepository.isNotificationsEnabled.first()) {
            notifyMissed(missedTasks)
        }

        return Result.success()
    }

    /** Records [day]'s quests still without a result 2 hours after their time, and returns them. */
    private suspend fun recordMissed(day: LocalDate, now: LocalDateTime): List<TaskEntity> {
        val loggedTaskIds = taskCompletionRepository.getLogsForDate(day).first().map { it.taskId }.toSet()
        return taskRepository.getTasksForDay(day).first()
            .filter { task ->
                task.id !in loggedTaskIds && now.isAfter(LocalDateTime.of(day, task.scheduledTime).plusHours(2))
            }
            // Recorded only if still without a result: a task completed a moment ago is left alone.
            .filter { task -> taskResultRecorder.recordNotDone(task.id, day) }
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
                manager.notify(MISSED_TAG, task.id.toInt(), notification)
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS not granted; the missed log is still recorded.
            }
        }
    }

    companion object {
        /**
         * Missed notices use the task id under this tag. A plain id of taskId * 10 + 3 collided
         * with other tasks' reminders (task 1's notice replaced task 13's reminder).
         */
        const val MISSED_TAG = "missed"
    }
}
