package com.pixelquest.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Quest Reminder"
        if (taskId == -1L) return

        try {
            val notification = NotificationHelper.buildTaskReminderNotification(
                context = context,
                taskId = taskId,
                taskName = taskName
            )
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }
}
