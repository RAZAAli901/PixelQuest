package com.pixelquest.app.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.notification.TaskActionReceiver
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.prompt.TaskPromptActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Quest Reminder"
        if (taskId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // An alarm armed before notifications were switched off must not post anything.
                if (settingsRepository.isNotificationsEnabled.first()) {
                    postReminder(context, taskId, taskName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postReminder(context: Context, taskId: Long, taskName: String) {
        val promptIntent = Intent(context, TaskPromptActivity::class.java).apply {
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_TASK_NAME", taskName)
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

        try {
            val notification = NotificationHelper.buildTaskReminderNotification(
                context = context,
                taskId = taskId,
                taskName = taskName,
                contentIntent = contentPendingIntent,
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
