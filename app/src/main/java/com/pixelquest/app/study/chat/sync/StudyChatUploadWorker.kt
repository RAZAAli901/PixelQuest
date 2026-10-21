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
import androidx.work.workDataOf
import com.pixelquest.app.study.chat.data.ChatUploadScheduler
import com.pixelquest.app.study.chat.data.ChatUploads
import com.pixelquest.app.study.chat.data.UploadRun
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends one file shared in a study chat (docs/study-chat.md), with a connection: [ChatUploads.run]
 * from wherever it got to. No connection on the way, WorkManager tries again later, backing off.
 */
@HiltWorker
class StudyChatUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val uploads: ChatUploads
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val clientId = inputData.getString(KEY_UPLOAD) ?: return Result.failure()
        return when (uploads.run(clientId)) {
            UploadRun.DONE -> Result.success()
            UploadRun.RETRY_LATER -> Result.retry()
        }
    }

    companion object {
        const val KEY_UPLOAD = "chat_upload"
        fun workName(clientId: String) = "pixelquest_study_chat_upload_$clientId"
    }
}

@Singleton
class WorkManagerChatUploadScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ChatUploadScheduler {

    override fun schedule(clientId: String) {
        val request = OneTimeWorkRequestBuilder<StudyChatUploadWorker>()
            .setInputData(workDataOf(StudyChatUploadWorker.KEY_UPLOAD to clientId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(StudyChatUploadWorker.workName(clientId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(clientId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(StudyChatUploadWorker.workName(clientId))
    }
}
