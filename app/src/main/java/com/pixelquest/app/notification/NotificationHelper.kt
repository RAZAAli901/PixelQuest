package com.pixelquest.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.pixelquest.app.R

object NotificationHelper {

    const val CHANNEL_ID = "pixelquest_reminders_channel"
    const val CHANNEL_NAME = "PixelQuest Reminders"
    const val CHANNEL_DESCRIPTION = "Notifications for task reminders and quest completion prompts"

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
}
