package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.domain.namaz.AutoMarkMissedUseCase
import com.pixelquest.app.util.AppClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Hourly, writes MISSED for prayers left pending after the next prayer started (AutoMarkMissedUseCase).
 * The tracker already shows them missed on time; this keeps the stored history right for the
 * weekly view and backups. Does nothing while the Namaz Tracker is off.
 */
@HiltWorker
class NamazMissedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val autoMarkMissed: AutoMarkMissedUseCase,
    private val clock: AppClock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        autoMarkMissed(clock.now().atZone(ZoneId.systemDefault()))
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "NamazMissedHourly"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NamazMissedWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
