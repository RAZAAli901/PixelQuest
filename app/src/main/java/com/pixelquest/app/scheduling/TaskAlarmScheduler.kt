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

    fun calculateTriggerTimeMillis(task: TaskEntity): Long {
        val now = LocalDateTime.now()
        var scheduledDateTime = LocalDateTime.of(task.scheduledDay, task.scheduledTime)
        if (scheduledDateTime.isBefore(now)) {
            scheduledDateTime = scheduledDateTime.plusDays(1)
        }
        return scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun calculateNextOccurrenceDate(task: TaskEntity, fromDate: LocalDate = LocalDate.now()): LocalDate {
        return when (task.recurrenceType) {
            RecurrenceType.DAILY -> fromDate.plusDays(1)
            RecurrenceType.WEEKLY -> fromDate.plusWeeks(1)
            RecurrenceType.MONTHLY -> fromDate.plusMonths(1)
            RecurrenceType.ONE_TIME -> task.scheduledDay
        }
    }

    fun scheduleExactAlarmForTask(task: TaskEntity) {
        if (!canScheduleExactAlarms()) {
            return
        }

        val triggerTimeMillis = calculateTriggerTimeMillis(task)
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

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    fun scheduleNextOccurrence(task: TaskEntity) {
        if (task.recurrenceType == RecurrenceType.ONE_TIME) return
        val nextDate = calculateNextOccurrenceDate(task)
        val updatedTask = task.copy(scheduledDay = nextDate)
        scheduleExactAlarmForTask(updatedTask)
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
}
