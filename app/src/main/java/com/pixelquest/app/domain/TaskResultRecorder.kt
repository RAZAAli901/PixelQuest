package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place that records whether a task was done on a day. Today, the reminder's buttons, the
 * full-screen prompt and the missed-task worker all go through it, so:
 * - a task has at most one result per day (the table also has a unique index),
 * - completing a task twice awards its XP once,
 * - a missed or skipped task can still be completed later that day, and earns XP then,
 * - nothing turns a completed task back into a missed one.
 */
@Singleton
class TaskResultRecorder @Inject constructor(
    private val taskCompletionRepository: TaskCompletionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository,
    private val taskRepository: com.pixelquest.app.domain.repository.TaskRepository
) {
    // Serialises a double tap, or the app and a notification button racing each other.
    private val mutex = Mutex()

    /** Marks the task done on [date]. Returns the XP awarded: 0 when it was already done. */
    suspend fun recordCompleted(taskId: Long, date: LocalDate): Int = mutex.withLock {
        // A deleted quest's reminder can still be in the shade; "Yes" there used to award XP and
        // leave a log for a quest that no longer exists.
        if (taskRepository.getTaskById(taskId).first() == null) return@withLock 0
        val existing = taskCompletionRepository.getLogForTaskOnDate(taskId, date)
        if (existing?.wasCompleted == true) return@withLock 0

        val streak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0
        val points = PointsCalculator.calculateXpForTask(currentStreak = streak)
        if (existing != null) {
            taskCompletionRepository.updateLog(existing.copy(wasCompleted = true, pointsAwarded = points))
        } else {
            taskCompletionRepository.insertLog(
                TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = true, pointsAwarded = points)
            )
        }
        userProfileRepository.getProfile().first()?.let { profile ->
            userProfileRepository.updateProfile(profile.copy(totalXp = profile.totalXp + points))
        }
        points
    }

    /**
     * Marks the task not done on [date] (skipped, or missed). Returns false, and changes nothing,
     * when the task already has a result that day.
     */
    suspend fun recordNotDone(taskId: Long, date: LocalDate): Boolean = mutex.withLock {
        if (taskRepository.getTaskById(taskId).first() == null) return@withLock false
        if (taskCompletionRepository.getLogForTaskOnDate(taskId, date) != null) return@withLock false
        taskCompletionRepository.insertLog(
            TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = false, pointsAwarded = 0)
        ) != -1L
    }
}
