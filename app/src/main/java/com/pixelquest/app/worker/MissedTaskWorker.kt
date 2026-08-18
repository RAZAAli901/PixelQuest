package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

@HiltWorker
class MissedTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tasks = taskRepository.getTasksForDay(today).first()
        val logs = taskCompletionRepository.getLogsForDate(today).first()
        val loggedTaskIds = logs.map { it.taskId }.toSet()

        val missedTasks = tasks.filter { task ->
            if (loggedTaskIds.contains(task.id)) return@filter false
            val scheduledDateTime = LocalDateTime.of(today, task.scheduledTime)
            val cutoffTime = scheduledDateTime.plusHours(2)
            now.isAfter(cutoffTime)
        }

        missedTasks.forEach { task ->
            val missedLog = TaskCompletionLogEntity(
                taskId = task.id,
                completedAt = LocalDateTime.now(),
                wasCompleted = false
            )
            taskCompletionRepository.logTaskCompletion(missedLog)
        }

        return Result.success()
    }
}
