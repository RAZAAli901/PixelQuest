package com.pixelquest.app.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.notification.NotificationContentBuilder
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.notification.ReminderContext
import com.pixelquest.app.notification.TaskActionReceiver
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.prompt.TaskPromptActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var taskAlarmScheduler: TaskAlarmScheduler

    @Inject
    lateinit var streakRepository: StreakRepository

    @Inject
    lateinit var taskCompletionRepository: TaskCompletionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Quest Reminder"
        if (taskId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // An alarm armed before notifications were switched off must not post anything.
                if (!settingsRepository.isNotificationsEnabled.first()) return@launch
                // Skip alarms left over from deleted or deactivated tasks.
                val task = taskRepository.getTaskById(taskId).first()
                if (task == null || !task.isActive) return@launch

                postReminder(context, taskId, task.name.ifBlank { taskName }, task.category)
                // Exact alarms fire once, so arm the next daily/weekly/monthly occurrence now.
                taskAlarmScheduler.scheduleNextOccurrence(task)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun postReminder(
        context: Context,
        taskId: Long,
        taskName: String,
        category: TaskCategory
    ) {
        val isSimpleMode = settingsRepository.simpleModeEnabled.first()
        val soundEnabled = settingsRepository.isNotificationSoundEnabled.first()
        val vibrationEnabled = settingsRepository.isNotificationVibrationEnabled.first()
        val copy = NotificationContentBuilder.reminder(buildReminderContext(taskName, category, isSimpleMode))

        val promptIntent = Intent(context, TaskPromptActivity::class.java).apply {
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_NAME", taskName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            promptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val yesIntent = Intent(context, TaskActionReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_WAS_COMPLETED", true)
        }
        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 1).toInt(),
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noIntent = Intent(context, TaskActionReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_WAS_COMPLETED", false)
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 10 + 2).toInt(),
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_SNOOZE
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_NAME", taskName)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            TaskAlarmScheduler.snoozeRequestCode(taskId),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val notification = NotificationHelper.buildTaskReminderNotification(
                context = context,
                taskId = taskId,
                taskName = taskName,
                contentIntent = contentPendingIntent,
                yesIntent = yesPendingIntent,
                noIntent = noPendingIntent,
                snoozeIntent = snoozePendingIntent,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                isSimpleMode = isSimpleMode,
                copy = copy
            )
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    private suspend fun buildReminderContext(
        taskName: String,
        category: TaskCategory,
        isSimpleMode: Boolean
    ): ReminderContext {
        val today = LocalDate.now()
        val tasksToday = taskRepository.getTasksForDay(today).first()
        val doneIds = taskCompletionRepository.getLogsForDate(today).first()
            .filter { it.wasCompleted }
            .map { it.taskId }
            .toSet()
        return ReminderContext(
            taskName = taskName,
            category = category,
            isSimpleMode = isSimpleMode,
            currentStreak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0,
            doneToday = tasksToday.count { it.id in doneIds },
            totalToday = tasksToday.size
        )
    }
}
