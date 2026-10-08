package com.pixelquest.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var taskAlarmScheduler: TaskAlarmScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in REARM_ACTIONS) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Turning notifications off cancels every alarm; don't bring them back on reboot.
                    if (!settingsRepository.isNotificationsEnabled.first()) return@launch
                    val tasks = taskRepository.getAllTasks().first()
                    tasks.forEach { task ->
                        taskAlarmScheduler.scheduleExactAlarmForTask(task)
                    }
                } catch (e: Exception) {
                    // An uncaught exception here crashed the app on every boot.
                    android.util.Log.e("BootReceiver", "Could not re-arm reminders after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        /** AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31). */
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        /**
         * Broadcasts after which every reminder is armed again. Alarms are absolute times, so after a
         * time zone or clock change an 08:00 reminder would otherwise ring at the old zone's 08:00.
         */
        val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
    }
}
