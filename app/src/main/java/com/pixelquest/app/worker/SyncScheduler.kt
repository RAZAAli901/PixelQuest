package com.pixelquest.app.worker

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SyncScheduler {
    fun scheduleProfileSync(debounceMs: Long = SyncSchedulerImpl.DEFAULT_DEBOUNCE_MS)
}

@Singleton
class SyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncScheduler {

    override fun scheduleProfileSync(debounceMs: Long) {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val builder = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
        // The debounce is WorkManager's initial delay, not an in-memory one: a quest completed from
        // its notification is recorded in a receiver whose process can end right after, and a
        // pending coroutine delay ended with it. Each request replaces the one still waiting, so a
        // burst of completions still syncs once. (Expedited work can't be delayed.)
        if (debounceMs > 0) {
            builder.setInitialDelay(debounceMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        } else {
            builder.setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SYNC_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            builder.build()
        )
    }

    companion object {
        const val UNIQUE_SYNC_WORK_NAME = "pixelquest_profile_sync_work"
        const val DEFAULT_DEBOUNCE_MS = 1500L
    }
}
