package com.pixelquest.app.worker

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface SyncScheduler {
    fun scheduleProfileSync(debounceMs: Long = SyncSchedulerImpl.DEFAULT_DEBOUNCE_MS)
}

@Singleton
class SyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncScheduler {

    private val scope = CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    )
    private var debounceJob: Job? = null

    override fun scheduleProfileSync(debounceMs: Long) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_SYNC_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    companion object {
        const val UNIQUE_SYNC_WORK_NAME = "pixelquest_profile_sync_work"
        const val DEFAULT_DEBOUNCE_MS = 1500L
    }
}
