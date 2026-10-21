package com.pixelquest.app.study.chat.sync

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
import com.pixelquest.app.study.chat.data.ChatSendScheduler
import com.pixelquest.app.study.chat.data.StudyChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the chat messages waiting on the phone (docs/study-chat.md, On the phone), in the order
 * they were written, once there's a connection, as the study party's ticks go. While some still
 * can't go, WorkManager tries again later, backing off; refused ones are marked and not sent again.
 */
@HiltWorker
class StudyChatSendWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val chat: StudyChatRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = if (chat.sendPending().needsRetry) Result.retry() else Result.success()
}

@Singleton
class WorkManagerChatSendScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ChatSendScheduler {

    override fun scheduleSend() {
        val request = OneTimeWorkRequestBuilder<StudyChatSendWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        // Appended to a send already running: one written meanwhile goes next.
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val UNIQUE_WORK = "pixelquest_study_chat_send"
    }
}
