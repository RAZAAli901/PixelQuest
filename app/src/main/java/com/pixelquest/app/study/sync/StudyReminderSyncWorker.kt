package com.pixelquest.app.study.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.study.data.StudyReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings every study party's reminders to the phone, after sending what's waiting
 * (docs/study-reminders.md, Sync): every 30 minutes with a connection, so a reminder someone else
 * made is armed even while the app stays closed (the alarms follow the cache, StudyReminderAlarmSync).
 * No connection, it tries again later.
 */
@HiltWorker
class StudyReminderSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminders: StudyReminderRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = when (reminders.refreshAll()) {
        is SupabaseResult.Success -> Result.success()
        is SupabaseResult.NetworkError -> Result.retry()
        // Signed out, or the server said no: nothing to gain by trying again before the next run.
        else -> Result.success()
    }

    companion object {
        private const val PERIODIC_WORK = "pixelquest_study_reminders_sync"
        private const val SEND_WORK = "pixelquest_study_reminders_send"
        const val PERIOD_MINUTES = 30L

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Every 30 minutes with a connection; kept as it is if it's already scheduled. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudyReminderSyncWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Once, as soon as there's a connection: for a reminder made, changed or deleted offline. */
        fun sendSoon(context: Context) {
            val request = OneTimeWorkRequestBuilder<StudyReminderSyncWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(SEND_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

/** Asks for the reminders waiting on the phone to go once there's a connection. */
fun interface ReminderSendScheduler {
    fun scheduleSend()
}

@Singleton
class WorkManagerReminderSendScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderSendScheduler {
    override fun scheduleSend() = StudyReminderSyncWorker.sendSoon(context)
}
