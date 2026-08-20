package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.domain.StreakCalculator
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate


@HiltWorker
class StreakEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val streakRepository: StreakRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetDate = LocalDate.now().minusDays(1)
        val tasksForDay = taskRepository.getTasksForDay(targetDate).first()
        val logsForDay = taskCompletionRepository.getLogsForDate(targetDate).first()
        val difficulty = difficultySettingsRepository.getCurrentDifficulty().first()
        val threshold = difficulty?.perfectDayThreshold ?: 0.7f

        val isPerfect = StreakCalculator.isPerfectDay(
            logs = logsForDay,
            totalTaskCount = tasksForDay.size,
            threshold = threshold
        )

        val streak = streakRepository.getCurrentStreak().first() ?: com.pixelquest.app.data.local.entity.StreakEntity()

        if (isPerfect) {
            val newCurrent = streak.currentStreak + 1
            val newLongest = kotlin.math.max(streak.longestStreak, newCurrent)
            val updatedStreak = streak.copy(
                currentStreak = newCurrent,
                longestStreak = newLongest,
                lastCompletedDate = targetDate,
                perfectDaysCount = streak.perfectDaysCount + 1
            )
            streakRepository.updateStreak(updatedStreak)
        } else {
            /**
             * STREAK-BREAK RULE:
             * Breaking a streak resets currentStreak to 0 ONLY.
             * longestStreak, perfectDaysCount, and totalXp are strictly preserved.
             * Level progress (Day 6 scope) is tracked separately via totalXp/perfectDaysCount.
             */
            val updatedStreak = streak.copy(
                currentStreak = 0
            )
            streakRepository.updateStreak(updatedStreak)
        }


        return Result.success()
    }
}



