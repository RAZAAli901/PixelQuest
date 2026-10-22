package com.pixelquest.app.domain.tasks

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import java.time.LocalDate
import javax.inject.Inject

/**
 * Encapsulates the business rule for completing a task on a specific date:
 * recording the result, awarding XP, rearming recurring alarms, and clearing reminders.
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskResultRecorder: TaskResultRecorder,
    private val taskAlarmScheduler: TaskAlarmScheduler,
) {
    suspend operator fun invoke(task: TaskEntity, date: LocalDate): Boolean {
        // 0 means it was already done (a double tap, or completed from the reminder).
        if (taskResultRecorder.recordCompleted(task.id, date) == 0) return false
        if (task.recurrenceType == RecurrenceType.ONE_TIME) {
            taskAlarmScheduler.cancelAlarmForTask(task)
        } else {
            taskAlarmScheduler.scheduleNextOccurrence(task)
        }
        taskAlarmScheduler.clearReminder(task.id)
        return true
    }
}
