package com.pixelquest.app.study.sync

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
import com.pixelquest.app.study.data.StudyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the ticks and study time waiting on the phone (docs/study-party.md), in the order they were
 * made, once there's a connection: as Darood's entries go. While some still can't go, WorkManager
 * tries again later, backing off; refused ones are marked and not sent again.
 */
@HiltWorker
class StudySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: StudyRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
        if (repository.sendPending().needsRetry) Result.retry() else Result.success()
}

/** Asks for the waiting ticks and study time to be sent once there's a connection. */
fun interface StudySyncScheduler {
    fun scheduleSend()
}

@Singleton
class WorkManagerStudySyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : StudySyncScheduler {

    override fun scheduleSend() {
        val request = OneTimeWorkRequestBuilder<StudySyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        // Appended to a send already running, as Darood's: one made meanwhile goes next.
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val UNIQUE_WORK = "pixelquest_study_sync"
    }
}
