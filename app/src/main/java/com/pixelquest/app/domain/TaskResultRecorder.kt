package com.pixelquest.app.domain

import com.pixelquest.app.config.GameConfig
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
 * - nothing turns a completed task back into a missed one,
 * - every completion that earns XP is synced to the cloud profile (only Today used to ask for that,
 *   so a quest done from its reminder reached the leaderboard only with the next in-app one).
 */
@Singleton
class TaskResultRecorder @Inject constructor(
    private val taskCompletionRepository: TaskCompletionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val streakRepository: StreakRepository,
    private val taskRepository: com.pixelquest.app.domain.repository.TaskRepository,
    private val syncScheduler: com.pixelquest.app.worker.SyncScheduler? = null,
) {
    // Serialises a double tap, or the app and a notification button racing each other.
    private val mutex = Mutex()

    sealed class CompletionResult {
        data class Success(val points: Int) : CompletionResult()
        object AlreadyCompleted : CompletionResult()
        object TaskNotFound : CompletionResult()
    }

    suspend fun recordTaskCompletion(taskId: Long, date: LocalDate): CompletionResult = mutex.withLock {
        // A deleted quest's reminder can still be in the shade; "Yes" there used to award XP and
        // leave a log for a quest that no longer exists.
        if (taskRepository.getTaskById(taskId).first() == null) return@withLock CompletionResult.TaskNotFound
        val existing = taskCompletionRepository.getLogForTaskOnDate(taskId, date)
        if (existing?.wasCompleted == true) return@withLock CompletionResult.AlreadyCompleted

        // B06: The first completion of a quest awards 0 XP (FIRST_COMPLETION_XP);
        // every later completion awards normal XP. Detected from existing completion logs.
        val hasPriorCompletion = taskCompletionRepository.getLogsForTask(taskId).first().any { it.wasCompleted }
        val streak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0
        val points = if (!hasPriorCompletion) {
            GameConfig.FIRST_COMPLETION_XP
        } else {
            PointsCalculator.calculateXpForTask(currentStreak = streak)
        }

        if (existing != null) {
            taskCompletionRepository.updateLog(existing.copy(wasCompleted = true, pointsAwarded = points))
        } else {
            taskCompletionRepository.insertLog(
                TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = true, pointsAwarded = points),
            )
        }
        if (points > 0) {
            userProfileRepository.addXp(points)
        }
        syncScheduler?.scheduleProfileSync()
        CompletionResult.Success(points)
    }

    /** Marks the task done on [date]. Returns the XP awarded: 0 when it was already done. */
    suspend fun recordCompleted(taskId: Long, date: LocalDate): Int {
        val result = recordTaskCompletion(taskId, date)
        return if (result is CompletionResult.Success) result.points else 0
    }

    /**
     * Marks the task not done on [date] (skipped, or missed). Returns false, and changes nothing,
     * when the task already has a result that day.
     */
    suspend fun recordNotDone(taskId: Long, date: LocalDate): Boolean = mutex.withLock {
        if (taskRepository.getTaskById(taskId).first() == null) return@withLock false
        if (taskCompletionRepository.getLogForTaskOnDate(taskId, date) != null) return@withLock false
        taskCompletionRepository.insertLog(
            TaskCompletionLogEntity(taskId = taskId, completedDate = date, wasCompleted = false, pointsAwarded = 0),
        ) != -1L
    }
}
