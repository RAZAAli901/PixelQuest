package com.pixelquest.app.darood.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.pixelquest.app.darood.data.DaroodPartyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the Darood entries waiting on the phone (docs/darood-design.md), in the order they were
 * posted, once there's a connection. While some still can't go (no connection, or the sign-in
 * needs refreshing), WorkManager tries again later, backing off. Refused entries are marked and
 * not sent again, so they never keep it retrying.
 */
@HiltWorker
class DaroodSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DaroodPartyRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
        if (repository.sendPending().needsRetry) Result.retry() else Result.success()
}

/** Asks for the waiting Darood entries to be sent once there's a connection. */
fun interface DaroodSyncScheduler {
    fun scheduleSend()
}

@Singleton
class WorkManagerDaroodSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : DaroodSyncScheduler {

    override fun scheduleSend() {
        val request = OneTimeWorkRequestBuilder<DaroodSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        // Appended to a send already running rather than replacing it: an entry posted meanwhile
        // goes next, and a send is never cut off halfway.
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val UNIQUE_WORK = "pixelquest_darood_sync"
    }
}
