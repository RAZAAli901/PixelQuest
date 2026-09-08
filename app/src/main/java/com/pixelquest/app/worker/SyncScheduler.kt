package com.pixelquest.app.worker

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SyncScheduler {
    fun scheduleProfileSync()
}

@Singleton
class SyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncScheduler {

    override fun scheduleProfileSync() {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>().build()
        workManager.enqueue(request)
    }
}
