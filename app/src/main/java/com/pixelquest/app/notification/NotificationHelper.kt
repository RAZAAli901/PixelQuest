package com.pixelquest.app.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.R

object NotificationHelper {

    /** Default reminder channel; see [NotificationChannels] for the full set. */
    val CHANNEL_ID: String = NotificationChannels.Spec.REMINDERS.id

    /**
     * Accent color for system notifications.
     * Uses PixelQuest Daylight Gold / Retro Amber (0xFFB45309), providing >= 4.5:1 contrast
     * against both dark (5.2:1) and light (4.6:1) OS notification shades.
     */
    const val NOTIFICATION_ACCENT_COLOR = 0xFFB45309.toInt()

    /** Reminders share a group so several due at once collapse under one summary. */
    const val REMINDER_GROUP_KEY = "com.pixelquest.app.REMINDERS"
    const val REMINDER_SUMMARY_ID = 900_001

    /** Creates every PixelQuest channel and removes the pre-Day 26 single channel. */
    fun createNotificationChannel(context: Context) {
        NotificationChannels.createAll(context)
    }

    fun getReminderTitle(taskName: String, isSimpleMode: Boolean): String {
        return if (isSimpleMode) "Time to do: $taskName" else "⚔️ Quest Time: $taskName"
    }

    fun getReminderText(taskName: String, isSimpleMode: Boolean): String {
        return if (isSimpleMode) "Time to complete: $taskName" else "Did you complete this quest today? Keep your streak!"
    }

    fun buildTaskReminderNotification(
        context: Context,
        taskId: Long,
        taskName: String,
        contentIntent: PendingIntent? = null,
        yesIntent: PendingIntent? = null,
        noIntent: PendingIntent? = null,
        snoozeIntent: PendingIntent? = null,
        fullScreenIntent: PendingIntent? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        isSimpleMode: Boolean = false,
        copy: NotificationCopy? = null
    ): Notification {
        val builder = NotificationCompat.Builder(context, NotificationChannels.reminderChannel(soundEnabled).id)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(copy?.title ?: getReminderTitle(taskName, isSimpleMode))
            .setContentText(copy?.text ?: getReminderText(taskName, isSimpleMode))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(REMINDER_GROUP_KEY)
            .setAutoCancel(true)
        if (copy != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(copy.bigText))
        }
        if (fullScreenIntent != null) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        if (!soundEnabled) {
            builder.setSound(null)
        }
        if (!vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L))
        }

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }
        val yesActionText = if (isSimpleMode) "Completed" else "Yes, I did it"
        if (yesIntent != null) {
            builder.addAction(0, yesActionText, yesIntent)
        }
        if (noIntent != null) {
            builder.addAction(0, "Not yet", noIntent)
        }
        if (snoozeIntent != null) {
            builder.addAction(0, "Snooze 10 min", snoozeIntent)
        }

        return builder.build()
    }

    /** Android 14 made full-screen intents opt-in for apps that aren't alarms or calls. */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 34) return true
        val system = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return system.canUseFullScreenIntent()
    }

    fun getMissedTaskTitle(taskName: String, isSimpleMode: Boolean): String {
        return if (isSimpleMode) "Task Missed: $taskName" else "💔 Quest Missed: $taskName"
    }

    // Missing one task doesn't always break the streak (perfect days use a percentage),
    // so the copy doesn't claim it did.
    fun getMissedTaskText(taskName: String, isSimpleMode: Boolean): String {
        return if (isSimpleMode) {
            "$taskName was due earlier today. You can still log it in the app."
        } else {
            "$taskName slipped past its time. Log it now or start fresh tomorrow."
        }
    }

    fun buildMissedTaskNotification(
        context: Context,
        taskId: Long,
        taskName: String,
        contentIntent: PendingIntent? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        isSimpleMode: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(context, NotificationChannels.Spec.MISSED.id)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(getMissedTaskTitle(taskName, isSimpleMode))
            .setContentText(getMissedTaskText(taskName, isSimpleMode))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (!soundEnabled) {
            builder.setSound(null)
        }
        if (!vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L))
        }
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }
        return builder.build()
    }

    /**
     * Posts (or clears) the reminder group summary based on which reminders are still showing.
     * Android only bundles a group on screen when a summary exists.
     */
    fun updateReminderGroupSummary(context: Context, isSimpleMode: Boolean, soundEnabled: Boolean) {
        val system = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val active = system.activeNotifications.filter {
            it.notification.group == REMINDER_GROUP_KEY && it.id != REMINDER_SUMMARY_ID
        }
        val manager = NotificationManagerCompat.from(context)
        if (active.size < 2) {
            manager.cancel(REMINDER_SUMMARY_ID)
            return
        }
        val noun = if (isSimpleMode) "tasks" else "quests"
        val inbox = NotificationCompat.InboxStyle()
        active.mapNotNull { it.notification.extras.getCharSequence(Notification.EXTRA_TITLE) }
            .forEach { inbox.addLine(it) }
        val summary = NotificationCompat.Builder(context, NotificationChannels.reminderChannel(soundEnabled).id)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NOTIFICATION_ACCENT_COLOR)
            .setContentTitle("${active.size} $noun due")
            .setStyle(inbox.setSummaryText("PixelQuest"))
            .setGroup(REMINDER_GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(REMINDER_SUMMARY_ID, summary)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }
}
