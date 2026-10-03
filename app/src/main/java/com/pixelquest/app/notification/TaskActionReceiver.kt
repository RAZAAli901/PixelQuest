package com.pixelquest.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskResultRecorder: TaskResultRecorder

    @Inject
    lateinit var taskAlarmScheduler: TaskAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val wasCompleted = intent.getBooleanExtra("EXTRA_WAS_COMPLETED", false)
        if (taskId == -1L) return

        if (intent.action == ACTION_SNOOZE) {
            val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Quest Reminder"
            taskAlarmScheduler.scheduleSnooze(taskId, taskName)
            NotificationManagerCompat.from(context).cancel(taskId.toInt())
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Does nothing if the task already has today's result, e.g. completed in the app.
                // "Not yet" records nothing: the task can still be done today, and the missed-task
                // check marks it missed only once its time has well passed.
                if (wasCompleted) {
                    taskResultRecorder.recordCompleted(taskId, LocalDate.now())
                }
            } finally {
                taskAlarmScheduler.clearReminder(taskId)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.pixelquest.app.action.SNOOZE_REMINDER"
    }
}
