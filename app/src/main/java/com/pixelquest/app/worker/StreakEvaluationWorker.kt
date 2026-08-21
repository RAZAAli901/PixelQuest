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


import com.pixelquest.app.domain.LevelCalculator
import com.pixelquest.app.domain.repository.UserProfileRepository

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.domain.repository.LevelHistoryRepository

import com.pixelquest.app.domain.LevelUpSignalManager

@HiltWorker
class StreakEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val streakRepository: StreakRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val levelHistoryRepository: LevelHistoryRepository,
    private val levelUpSignalManager: LevelUpSignalManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetDate = LocalDate.now(java.time.ZoneId.systemDefault()).minusDays(1)
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

        // Idempotency safeguard: if yesterday was already evaluated, skip re-evaluating
        if (streak.lastCompletedDate == targetDate) {
            return Result.success()
        }

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

            val profile = userProfileRepository.getProfile().first()
            if (profile != null) {
                val newProgress = profile.perfectDaysTowardNextLevel + 1
                val daysRequired = difficulty?.daysRequiredPerLevel ?: 7
                if (LevelCalculator.shouldLevelUp(newProgress, daysRequired)) {
                    val newLevel = profile.level + 1
                    userProfileRepository.updateProfile(
                        profile.copy(
                            level = newLevel,
                            perfectDaysTowardNextLevel = LevelCalculator.getPostLevelUpProgress()
                        )
                    )
                    levelHistoryRepository.insertLevelHistory(
                        LevelHistoryEntity(
                            level = newLevel,
                            achievedDate = System.currentTimeMillis(),
                            difficultyAtTimeOfLevelUp = difficulty?.difficultyLevel?.name ?: "MEDIUM"
                        )
                    )
                    levelUpSignalManager.setPendingLevelUp(newLevel)
                } else {
                    userProfileRepository.updateProfile(
                        profile.copy(perfectDaysTowardNextLevel = newProgress)
                    )
                }
            }
        } else {
            /**
             * STREAK-BREAK RULE:
             * Breaking a streak resets currentStreak to 0 ONLY.
             * longestStreak, perfectDaysCount, and totalXp are strictly preserved.
             * Level progress (Day 6 scope) is tracked separately via totalXp/perfectDaysCount.
             */
            val updatedStreak = streak.copy(
                currentStreak = 0,
                lastCompletedDate = targetDate
            )
            streakRepository.updateStreak(updatedStreak)
        }



        return Result.success()
    }
}



