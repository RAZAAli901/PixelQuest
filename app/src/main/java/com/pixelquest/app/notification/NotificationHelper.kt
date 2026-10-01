package com.pixelquest.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
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
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        isSimpleMode: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(getReminderTitle(taskName, isSimpleMode))
            .setContentText(getReminderText(taskName, isSimpleMode))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        val yesActionText = if (isSimpleMode) "Completed" else "Yes, I did it"
        if (yesIntent != null) {
            builder.addAction(0, yesActionText, yesIntent)
        }
        if (noIntent != null) {
            builder.addAction(0, "Not yet", noIntent)
        }

        return builder.build()
    }

    fun getMissedTaskTitle(taskName: String, isSimpleMode: Boolean): String {
        return if (isSimpleMode) "Task Missed: $taskName" else "💔 Quest Missed: $taskName"
    }

    fun getMissedTaskText(taskName: String, isSimpleMode: Boolean): String {
        return if (isSimpleMode) {
            "You missed a scheduled task: $taskName."
        } else {
            "You broke your streak on $taskName! Start a new streak tomorrow."
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
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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
}
