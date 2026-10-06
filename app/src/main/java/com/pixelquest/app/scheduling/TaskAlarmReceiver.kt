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
import com.pixelquest.app.domain.model.ReminderStyle
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
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.prefs.EncouragementPackStore
import com.pixelquest.app.domain.ai.AiEncouragementProvider
import com.pixelquest.app.domain.ai.EncouragementMessageProvider
import com.pixelquest.app.domain.ai.StaticEncouragementBank
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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

    @Inject
    lateinit var encouragementPackStore: EncouragementPackStore

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
                if (task == null || !task.isActive || !task.reminderEnabled) return@launch

                val occurrenceDate = LocalDateTime.now().plusMinutes(task.reminderLeadMinutes.toLong()).toLocalDate()
                // Stay quiet if the task already has a result for that day (done or skipped in the
                // app, or answered from an earlier reminder or a snooze).
                if (taskCompletionRepository.getLogForTaskOnDate(taskId, occurrenceDate) == null) {
                    postReminder(
                        context = context,
                        taskId = taskId,
                        taskName = task.name.ifBlank { taskName },
                        category = task.category,
                        style = task.reminderStyle,
                        startsInMinutes = minutesUntilTask(task)
                    )
                }
                // Exact alarms fire once, so arm the next daily/weekly/monthly occurrence now.
                taskAlarmScheduler.scheduleNextOccurrence(task, handledDate = occurrenceDate)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun postReminder(
        context: Context,
        taskId: Long,
        taskName: String,
        category: TaskCategory,
        style: ReminderStyle,
        startsInMinutes: Int
    ) {
        val isSimpleMode = settingsRepository.simpleModeEnabled.first()
        // A SILENT task stays quiet even when reminder sound is on.
        val soundEnabled = settingsRepository.isNotificationSoundEnabled.first() && style != ReminderStyle.SILENT
        val vibrationEnabled = settingsRepository.isNotificationVibrationEnabled.first()
        val copy = NotificationContentBuilder.reminder(
            buildReminderContext(taskName, category, isSimpleMode).copy(
                startsInMinutes = startsInMinutes,
                encouragement = encouragementProvider().messageFor(LocalDate.now(), taskId, isSimpleMode)
            )
        )

        val promptIntent = Intent(context, TaskPromptActivity::class.java).apply {
            putExtra(com.pixelquest.app.ui.prompt.PromptQueue.EXTRA_TASK_ID, taskId)
            putExtra(com.pixelquest.app.ui.prompt.PromptQueue.EXTRA_TASK_NAME, taskName)
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
                fullScreenIntent = contentPendingIntent.takeIf {
                    style == ReminderStyle.PROMPT && NotificationHelper.canUseFullScreenIntent(context)
                },
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                isSimpleMode = isSimpleMode,
                copy = copy
            )
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskId.toInt(), notification)
            NotificationHelper.updateReminderGroupSummary(context, isSimpleMode, soundEnabled)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    /** AI-written lines only while both AI Coach and AI reminder messages are on. */
    private suspend fun encouragementProvider(): EncouragementMessageProvider {
        val useAi = settingsRepository.aiInsightsEnabled.first() &&
            settingsRepository.aiReminderMessagesEnabled.first()
        return if (useAi) AiEncouragementProvider({ encouragementPackStore.load() }) else StaticEncouragementBank
    }

    /** Minutes from now to the task's time on the day this reminder is for (0 if it is due now). */
    private fun minutesUntilTask(task: TaskEntity): Int {
        if (task.reminderLeadMinutes <= 0) return 0
        val now = LocalDateTime.now()
        val taskDate = now.plusMinutes(task.reminderLeadMinutes.toLong()).toLocalDate()
        val minutes = Duration.between(now, LocalDateTime.of(taskDate, task.scheduledTime)).toMinutes()
        return if (minutes >= 1) minutes.toInt() else 0
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
