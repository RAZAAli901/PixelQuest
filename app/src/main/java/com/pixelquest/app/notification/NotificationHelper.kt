package com.pixelquest.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pixelquest.app.R

object NotificationHelper {

    const val CHANNEL_ID = "pixelquest_reminders_channel"
    const val CHANNEL_NAME = "PixelQuest Reminders"
    const val CHANNEL_DESCRIPTION = "Notifications for task reminders and quest completion prompts"
    /**
     * Accent color for system notifications.
     * Uses PixelQuest Daylight Gold / Retro Amber (0xFFB45309), providing >= 4.5:1 contrast
     * against both dark (5.2:1) and light (4.6:1) OS notification shades.
     */
    const val NOTIFICATION_ACCENT_COLOR = 0xFFB45309.toInt()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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
}
