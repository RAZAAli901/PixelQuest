package com.pixelquest.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.StreakAtRisk
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.util.AppClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * The evening nudge on the "Streaks and progress" channel: once a day, around 19:00, if the player
 * has a streak to lose and today's quests aren't yet enough to keep it, say how many more are needed.
 */
@HiltWorker
class StreakAtRiskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val streakRepository: StreakRepository,
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val difficultySettingsRepository: DifficultySettingsRepository,
    private val appClock: AppClock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!settingsRepository.isNotificationsEnabled.first()) return Result.success()
        val now = appClock.now()
        val today = now.toLocalDate()
        // Too early to warn (a run that WorkManager brought forward), or already sent today.
        if (now.toLocalTime().isBefore(EARLIEST)) return Result.success()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_SENT, null) == today.toString()) return Result.success()

        val streak = streakRepository.getCurrentStreak().first()?.currentStreak ?: 0
        val dueIds = taskRepository.getTasksForDay(today).first().map { it.id }.toSet()
        val done = taskCompletionRepository.getLogsForDate(today).first()
            .count { it.wasCompleted && it.taskId in dueIds }
        val threshold = DifficultyMode.perfectDayThreshold(difficultySettingsRepository.getCurrentDifficulty().first())
        val needed = StreakAtRisk.questsStillNeeded(streak, dueIds.size, done, threshold) ?: return Result.success()

        val (title, text) = StreakAtRisk.copy(streak, needed, settingsRepository.simpleModeEnabled.first())
        val openApp = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.Spec.PROGRESS.id)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NotificationHelper.NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            prefs.edit().putString(KEY_LAST_SENT, today.toString()).apply()
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 900_003
        const val PREFS_NAME = "pixelquest_streak_nudge"
        const val KEY_LAST_SENT = "key_last_sent_date"
        private const val WORK_NAME = "StreakAtRiskWorkerPeriodic"
        private val EARLIEST: LocalTime = LocalTime.of(18, 0)
        private val SEND_AT: LocalTime = LocalTime.of(19, 0)

        /** Minutes from [now] to the next 19:00. */
        fun delayUntilNextRun(now: LocalDateTime): Long {
            val todayAt = now.toLocalDate().atTime(SEND_AT)
            val next = if (now.isBefore(todayAt)) todayAt else todayAt.plusDays(1)
            return Duration.between(now, next).toMinutes()
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StreakAtRiskWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextRun(LocalDateTime.now()), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
