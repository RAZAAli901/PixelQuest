package com.pixelquest.app.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.notification.TaskActionReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Quest Reminder"
        if (taskId == -1L) return

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

        try {
            val notification = NotificationHelper.buildTaskReminderNotification(
                context = context,
                taskId = taskId,
                taskName = taskName,
                yesIntent = yesPendingIntent,
                noIntent = noPendingIntent
            )
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission missing
        }
    }
}
