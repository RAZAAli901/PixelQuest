package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pixelquest.app.domain.DayOutcome
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
import com.pixelquest.app.domain.repository.SettingsRepository

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
    private val levelUpSignalManager: LevelUpSignalManager,
    private val syncScheduler: com.pixelquest.app.worker.SyncScheduler? = null,
    private val settingsRepository: SettingsRepository? = null
) : CoroutineWorker(appContext, workerParams) {

    /**
     * ARCHITECTURAL CONTRACT (Day 18 Simple Mode):
     * StreakEvaluationWorker continues to execute normal nightly evaluation regardless of Simple Mode state.
     * Under Simple Mode, streak records, perfect day counts, and streak break states continue to be calculated
     * and persisted to Room DB so that historical consistency is preserved if a user later switches back
     * to Full Game Mode.
     */
    override suspend fun doWork(): Result = try {
        evaluate()
    } finally {
        // Each night queues the next one (see RUN_AT). WorkManager isn't initialised in unit tests.
        try { scheduleNextRun(applicationContext) } catch (e: IllegalStateException) { }
    }

    private suspend fun evaluate(): Result {
        val yesterday = LocalDate.now(java.time.ZoneId.systemDefault()).minusDays(1)
        val initial = streakRepository.getCurrentStreak().first() ?: com.pixelquest.app.data.local.entity.StreakEntity()

        // lastCompletedDate is the last day already evaluated. Catch up on every day since then
        // (the phone may have been off, or the worker delayed), up to MAX_CATCH_UP_DAYS.
        val firstDay = initial.lastCompletedDate?.plusDays(1) ?: yesterday
        if (firstDay.isAfter(yesterday)) return Result.success()
        val from = maxOf(firstDay, yesterday.minusDays(MAX_CATCH_UP_DAYS - 1))

        val difficulty = difficultySettingsRepository.getCurrentDifficulty().first()
        val threshold = com.pixelquest.app.domain.DifficultyMode.perfectDayThreshold(difficulty)

        var streak = initial
        var date = from
        while (!date.isAfter(yesterday)) {
            val scheduledIds = taskRepository.getTasksForDay(date).first().map { it.id }.toSet()
            val logs = taskCompletionRepository.getLogsForDate(date).first()
            streak = when (StreakCalculator.dayOutcome(scheduledIds, logs, threshold)) {
                DayOutcome.PERFECT -> {
                    addPerfectDayToLevel(difficulty)
                    streak.copy(
                        currentStreak = streak.currentStreak + 1,
                        longestStreak = kotlin.math.max(streak.longestStreak, streak.currentStreak + 1),
                        lastCompletedDate = date,
                        perfectDaysCount = streak.perfectDaysCount + 1
                    )
                }
                /**
                 * STREAK-BREAK RULE:
                 * Breaking a streak resets currentStreak to 0 ONLY.
                 * longestStreak, perfectDaysCount, and totalXp are strictly preserved.
                 */
                DayOutcome.MISSED -> streak.copy(currentStreak = 0, lastCompletedDate = date)
                // Nothing was scheduled: the streak carries over unchanged.
                DayOutcome.REST -> streak.copy(lastCompletedDate = date)
            }
            date = date.plusDays(1)
        }

        streakRepository.updateStreak(streak)
        if (streak.currentStreak != initial.currentStreak || streak.perfectDaysCount != initial.perfectDaysCount) {
            syncScheduler?.scheduleProfileSync()
        }
        return Result.success()
    }

    /** Counts one perfect day toward the next level, levelling up when enough have been reached. */
    private suspend fun addPerfectDayToLevel(difficulty: com.pixelquest.app.data.local.entity.DifficultySettingsEntity?) {
        val profile = userProfileRepository.getProfile().first() ?: return
        val newProgress = profile.perfectDaysTowardNextLevel + 1
        val daysRequired = com.pixelquest.app.domain.DifficultyMode.daysRequiredPerLevel(difficulty)
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
            val isSimpleMode = settingsRepository?.simpleModeEnabled?.first() ?: false
            levelUpSignalManager.setPendingLevelUp(newLevel, suppressCelebration = isSimpleMode)
        } else {
            userProfileRepository.updateProfile(profile.copy(perfectDaysTowardNextLevel = newProgress))
        }
    }

    companion object {
        /** Days evaluated at most in one run after a long gap. */
        const val MAX_CATCH_UP_DAYS = 60L

        /**
         * When yesterday is settled: after the 2-hour window of a quest as late as 23:59, so a "Yes"
         * for a late-evening quest answered after midnight still counts. (It ran at 00:05, when a 23:00
         * quest still had an hour left, and that answer never reached the streak.)
         */
        val RUN_AT: java.time.LocalTime = java.time.LocalTime.of(2, 5)
        private const val WORK_NAME = "StreakEvaluationWorkerNightly"
        private const val LEGACY_PERIODIC_WORK = "StreakEvaluationWorkerPeriodic"

        /**
         * Real time until the next [RUN_AT] in [zone]. Computed in the zone, so a clock change
         * doesn't move the run: the 24-hour periodic job it replaces drifted to 23:05 for the winter.
         */
        fun delayUntilNextRun(now: java.time.ZonedDateTime): java.time.Duration {
            val todayAt = now.toLocalDate().atTime(RUN_AT).atZone(now.zone)
            val next = if (now.isBefore(todayAt)) todayAt else now.toLocalDate().plusDays(1).atTime(RUN_AT).atZone(now.zone)
            return java.time.Duration.between(now, next)
        }

        private fun request() = androidx.work.OneTimeWorkRequestBuilder<StreakEvaluationWorker>()
            .setInitialDelay(delayUntilNextRun(java.time.ZonedDateTime.now()).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        /** At launch: start the nightly chain unless a run is already waiting. */
        fun schedule(context: android.content.Context) {
            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK)
            workManager.enqueueUniqueWork(WORK_NAME, androidx.work.ExistingWorkPolicy.KEEP, request())
        }

        /** From a run: queue the next night after this one finishes. */
        fun scheduleNextRun(context: android.content.Context) {
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE, request())
        }
    }
}
