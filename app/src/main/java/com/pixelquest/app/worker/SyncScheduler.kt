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

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
    )
    private var debounceJob: kotlinx.coroutines.Job? = null

    override fun scheduleProfileSync(debounceMs: Long) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            if (debounceMs > 0) {
                kotlinx.coroutines.delay(debounceMs)
            }

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
                .setConstraints(constraints)
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
