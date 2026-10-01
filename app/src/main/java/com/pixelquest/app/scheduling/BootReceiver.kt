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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Turning notifications off cancels every alarm; don't bring them back on reboot.
                    if (!settingsRepository.isNotificationsEnabled.first()) return@launch
                    val tasks = taskRepository.getAllTasks().first()
                    tasks.forEach { task ->
                        taskAlarmScheduler.scheduleExactAlarmForTask(task)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
