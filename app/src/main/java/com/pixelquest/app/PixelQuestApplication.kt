package com.pixelquest.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.worker.MissedTaskWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PixelQuestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Fast cold-start: lightweight notification channel creation
        NotificationHelper.createNotificationChannel(this)
        // Background async enqueue of periodic background workers
        scheduleMissedTaskWorker()
        scheduleStreakEvaluationWorker()
    }

    private fun scheduleMissedTaskWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MissedTaskWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MissedTaskWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleStreakEvaluationWorker() {
        val now = java.time.LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(5)
        val initialDelayMinutes = java.time.Duration.between(now, nextMidnight).toMinutes()

        val workRequest = PeriodicWorkRequestBuilder<com.pixelquest.app.worker.StreakEvaluationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StreakEvaluationWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

