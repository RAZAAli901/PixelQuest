package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Next reminder time for [task] in epoch millis, or null when it has no future occurrence.
     * [notBefore] skips occurrences before that date (used after the task is done for today).
     */
    fun nextTriggerTimeMillis(task: TaskEntity, notBefore: LocalDate? = null): Long? {
        val next = ReminderSchedule.nextTriggerAt(
            scheduledDay = task.scheduledDay,
            scheduledTime = task.scheduledTime,
            recurrence = task.recurrenceType,
            now = LocalDateTime.now(),
            leadMinutes = task.reminderLeadMinutes,
            notBefore = notBefore
        ) ?: return null
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun calculateTriggerTimeMillis(task: TaskEntity): Long {
        return nextTriggerTimeMillis(task)
            ?: LocalDateTime.of(task.scheduledDay, task.scheduledTime)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun calculateNextOccurrenceDate(task: TaskEntity, fromDate: LocalDate = LocalDate.now()): LocalDate {
        return when (task.recurrenceType) {
            RecurrenceType.DAILY -> fromDate.plusDays(1)
            RecurrenceType.WEEKLY -> fromDate.plusWeeks(1)
            RecurrenceType.MONTHLY -> fromDate.plusMonths(1)
            RecurrenceType.ONE_TIME -> task.scheduledDay
        }
    }

    fun scheduleExactAlarmForTask(task: TaskEntity, notBefore: LocalDate? = null) {
        if (!task.reminderEnabled) {
            // Reminders switched off for this task: make sure nothing is left armed.
            cancelAlarmForTask(task)
            return
        }
        if (!canScheduleExactAlarms()) {
            return
        }

        // A one-time task whose time has passed has nothing left to remind about.
        val triggerTimeMillis = nextTriggerTimeMillis(task, notBefore) ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_NAME", task.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            android.util.Log.w("TaskAlarmScheduler", "Exact alarm permission revoked or unavailable, degrading gracefully", e)
        } catch (e: Exception) {
            android.util.Log.e("TaskAlarmScheduler", "Failed to schedule alarm for task ${task.id}", e)
        }
    }

    /**
     * Arms the first occurrence after [handledDate], e.g. once that day's reminder has fired or the
     * task is done. With a lead time a reminder can fire the evening before its task, so the
     * receiver passes the task's date rather than today.
     */
    fun scheduleNextOccurrence(task: TaskEntity, handledDate: LocalDate = LocalDate.now()) {
        if (task.recurrenceType == RecurrenceType.ONE_TIME) return
        scheduleExactAlarmForTask(task, notBefore = handledDate.plusDays(1))
    }

    /**
     * One-off alarm [minutes] from now. Uses its own request code so it sits alongside the task's
     * regular alarm instead of replacing it.
     */
    fun scheduleSnooze(taskId: Long, taskName: String, minutes: Long = SNOOZE_MINUTES) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_NAME", taskName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            android.util.Log.w("TaskAlarmScheduler", "Could not schedule snooze for task $taskId", e)
        }
    }

    fun cancelAlarmForTask(task: TaskEntity) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllAlarms(tasks: List<TaskEntity>) {
        tasks.forEach { cancelAlarmForTask(it) }
    }

    fun rescheduleAllAlarms(tasks: List<TaskEntity>) {
        tasks.forEach { scheduleExactAlarmForTask(it) }
    }

    companion object {
        const val SNOOZE_MINUTES = 10L
        fun snoozeRequestCode(taskId: Long): Int = (taskId * 10 + 4).toInt()
    }
}
