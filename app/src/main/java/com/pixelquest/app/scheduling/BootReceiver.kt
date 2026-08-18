package com.pixelquest.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
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
