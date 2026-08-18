package com.pixelquest.app

import android.app.Application
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
        NotificationHelper.createNotificationChannel(this)
        scheduleMissedTaskWorker()
    }

    private fun scheduleMissedTaskWorker() {
        val workRequest = PeriodicWorkRequestBuilder<MissedTaskWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MissedTaskWorkerPeriodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
