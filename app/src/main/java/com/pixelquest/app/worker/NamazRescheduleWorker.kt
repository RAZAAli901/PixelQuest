package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.namaz.NamazReminders
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Once a day, arms the next 48 hours of prayer reminders again (docs/namaz-design.md, Reminders).
 * A safety net: the app, the midnight worker, each alarm and BootReceiver keep them armed, but a
 * phone that killed the app or cleared its alarms another way still gets them back within a day.
 * Rescheduling is idempotent, so it changes nothing when all is well.
 */
@HiltWorker
class NamazRescheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminders: NamazReminders
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        reminders.reschedule()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "NamazRescheduleDaily"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NamazRescheduleWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
