package com.pixelquest.app.domain.tasks

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import java.time.LocalDate
import javax.inject.Inject

/**
 * Encapsulates the business rule for skipping a task on a specific date:
 * recording not-done status, rearming recurring alarms, and clearing reminders.
 */
class SkipTaskUseCase @Inject constructor(
    private val taskResultRecorder: TaskResultRecorder,
    private val taskAlarmScheduler: TaskAlarmScheduler,
) {
    suspend operator fun invoke(task: TaskEntity, date: LocalDate): Boolean {
        if (!taskResultRecorder.recordNotDone(task.id, date)) return false
        if (task.recurrenceType == RecurrenceType.ONE_TIME) {
            taskAlarmScheduler.cancelAlarmForTask(task)
        } else {
            taskAlarmScheduler.scheduleNextOccurrence(task)
        }
        taskAlarmScheduler.clearReminder(task.id)
        return true
    }
}
