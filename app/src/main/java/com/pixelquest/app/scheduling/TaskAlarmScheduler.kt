package com.pixelquest.app.scheduling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskOccurrence
import com.pixelquest.app.domain.model.RecurrenceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmScheduler: AlarmScheduler,
) {
    constructor(@ApplicationContext context: Context) : this(context, AndroidAlarmScheduler(context))

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun openExactAlarmSettingsIntent(): Intent = alarmScheduler.openExactAlarmSettingsIntent()

    /**
     * Next reminder time for [task] in epoch millis, or null when it has no future occurrence.
     * [notBefore] skips occurrences before that date (used after the task is done for today).
     */
    fun nextTriggerTimeMillis(task: TaskEntity, notBefore: LocalDate? = null): Long? =
        nextReminder(task, notBefore)?.at?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    /** The next reminder for [task] and the day of the occurrence it is for. */
    fun nextReminder(task: TaskEntity, notBefore: LocalDate? = null): ReminderSchedule.NextReminder? =
        ReminderSchedule.nextReminder(
            scheduledDay = task.scheduledDay,
            scheduledTime = task.scheduledTime,
            recurrence = task.recurrenceType,
            now = LocalDateTime.now(),
            leadMinutes = task.reminderLeadMinutes,
            notBefore = notBefore,
            weeklyDays = task.weeklyDays,
        )

    fun calculateTriggerTimeMillis(task: TaskEntity): Long = nextTriggerTimeMillis(task)
        ?: LocalDateTime.of(task.scheduledDay, task.scheduledTime)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * The next date after [fromDate] on which [task] is due, using the same rule as the Today list
     * (including a weekly task's chosen days). A one-time task returns its own date.
     */
    fun calculateNextOccurrenceDate(task: TaskEntity, fromDate: LocalDate = LocalDate.now()): LocalDate {
        if (task.recurrenceType == RecurrenceType.ONE_TIME) return task.scheduledDay
        var date = maxOf(fromDate.plusDays(1), task.scheduledDay)
        // A monthly task is due within 31 days and any other recurring task within 7.
        repeat(400) {
            if (TaskOccurrence.occursOn(task.scheduledDay, task.recurrenceType, date, task.weeklyDays)) return date
            date = date.plusDays(1)
        }
        return date
    }

    fun scheduleExactAlarmForTask(task: TaskEntity, notBefore: LocalDate? = null) {
        if (!task.reminderEnabled || !task.isActive) {
            // Reminders switched off for this task, or the quest is paused: make sure nothing is
            // left armed. Turning notifications back on (and every boot) re-armed paused quests,
            // whose alarms then woke the device to show nothing.
            cancelAlarmForTask(task)
            return
        }
        // A one-time task whose time has passed has nothing left to remind about. A date the clock
        // can't represent (e.g. an edited backup's year 999999999 overflows toEpochMilli) is skipped
        // rather than crashing whoever is re-arming every reminder.
        // Never go back to an occurrence already handled (reminded, done or skipped): a cold start
        // inside a quest's lead window used to re-arm the same day's reminder at the task time.
        val firstAllowed = listOfNotNull(notBefore, lastHandled(task.id)?.plusDays(1)).maxOrNull()
        val next = try {
            nextReminder(task, firstAllowed)?.let { it to it.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        } catch (e: ArithmeticException) {
            com.pixelquest.app.util.AppLog.w("TaskAlarmScheduler", "Task ${task.id} has an unrepresentable date; no reminder armed", e)
            null
        } catch (e: java.time.DateTimeException) {
            com.pixelquest.app.util.AppLog.w("TaskAlarmScheduler", "Task ${task.id} has an unrepresentable date; no reminder armed", e)
            null
        } ?: return
        val (reminder, triggerTimeMillis) = next
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_NAME", task.name)
            putExtra(EXTRA_OCCURRENCE_DATE, reminder.occurrenceDate.toEpochDay())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val armed = alarmScheduler.schedule(triggerTimeMillis, pendingIntent)
        if (!armed) {
            com.pixelquest.app.util.AppLog.w("TaskAlarmScheduler", "Failed to schedule alarm for task ${task.id}")
        }
    }

    /**
     * Arms the first occurrence after [handledDate], e.g. once that day's reminder has fired or the
     * task is done. With a lead time a reminder can fire the evening before its task, so the
     * receiver passes the task's date rather than today.
     */
    fun scheduleNextOccurrence(task: TaskEntity, handledDate: LocalDate = LocalDate.now()) {
        markHandled(task.id, handledDate)
        if (task.recurrenceType == RecurrenceType.ONE_TIME) return
        scheduleExactAlarmForTask(task, notBefore = handledDate.plusDays(1))
    }

    private val handledPrefs by lazy { context.getSharedPreferences(HANDLED_PREFS, Context.MODE_PRIVATE) }

    /** The latest occurrence of [taskId] already reminded, done or skipped. */
    fun lastHandled(taskId: Long): LocalDate? =
        handledPrefs.getLong("task_$taskId", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let { LocalDate.ofEpochDay(it) }

    private fun markHandled(taskId: Long, date: LocalDate) {
        if (lastHandled(taskId)?.isAfter(date) == true) return
        handledPrefs.edit().putLong("task_$taskId", date.toEpochDay()).apply()
    }

    /** After an edit (a new time may need today's reminder after all). */
    fun forgetHandled(taskId: Long) {
        handledPrefs.edit().remove("task_$taskId").apply()
    }

    /**
     * One-off alarm [minutes] from now. Uses its own request code so it sits alongside the task's
     * regular alarm instead of replacing it.
     */
    fun scheduleSnooze(taskId: Long, taskName: String, minutes: Long = SNOOZE_MINUTES, occurrenceDate: LocalDate? = null) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZED_REMINDER
            occurrenceDate?.let { putExtra(EXTRA_OCCURRENCE_DATE, it.toEpochDay()) }
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_NAME", taskName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val armed = alarmScheduler.schedule(triggerAt, pendingIntent)
        if (!armed) {
            com.pixelquest.app.util.AppLog.w("TaskAlarmScheduler", "Could not schedule snooze for task $taskId")
        }
    }

    /**
     * Clears what is left of a reminder once its task has a result: the reminder or missed notice
     * still in the shade and a pending snooze, which would otherwise ring again for a task already done.
     */
    fun clearReminder(taskId: Long) {
        val notifications = androidx.core.app.NotificationManagerCompat.from(context)
        notifications.cancel(taskId.toInt())
        notifications.cancel(com.pixelquest.app.worker.MissedTaskWorker.MISSED_TAG, taskId.toInt())
        val snooze = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(taskId),
            Intent(context, TaskAlarmReceiver::class.java).setAction(ACTION_SNOOZED_REMINDER),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (snooze != null) {
            alarmScheduler.cancel(snooze)
        }
    }

    fun cancelAlarmForTask(task: TaskEntity) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmScheduler.cancel(pendingIntent)
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

        /** Shortest window Android 12+ honours for inexact alarms. */
        const val INEXACT_WINDOW_MS = 10 * 60_000L
        fun snoozeRequestCode(taskId: Long): Int = (taskId * 10 + 4).toInt()

        /**
         * Snoozes carry their own action. Android matches PendingIntents by request code and action
         * (not extras), and a regular reminder's code is the task id, so without it quest 1's snooze
         * (code 14) was quest 14's reminder: snoozing or finishing quest 1 replaced or cancelled it.
         */
        const val ACTION_SNOOZED_REMINDER = "com.pixelquest.app.action.SNOOZED_REMINDER"

        /**
         * The day of the quest occurrence a reminder is for (epoch day), carried from the alarm to the
         * notification's buttons and the prompt. Working it out from the time things happen went
         * wrong after midnight: a snooze firing at 00:02 handled the next day, and "Yes" tapped at
         * 00:15 for a 23:00 quest credited the new day.
         */
        const val EXTRA_OCCURRENCE_DATE = "EXTRA_OCCURRENCE_DATE"
        private const val HANDLED_PREFS = "pixelquest_reminders_handled"

        fun occurrenceDateFrom(intent: Intent?): LocalDate? =
            com.pixelquest.app.util.IntentExtras.epochDay(intent, EXTRA_OCCURRENCE_DATE)
    }
}
