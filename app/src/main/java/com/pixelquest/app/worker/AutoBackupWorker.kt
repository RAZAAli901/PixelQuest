package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.data.backup.AutoBackups
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.backup.DataExportImport
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Once a day, a backup of the player's progress kept on the phone (AutoBackups), so a corrupt
 * database or a mistake doesn't cost everything when no backup was exported. Nothing is saved
 * before there's something to keep (BackupPayload.hasProgress). Counting a fresh install's "missed"
 * starter quests saved a new player's empty game, which the welcome screen then offered to restore.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRestorer: BackupRestorer,
    private val autoBackups: AutoBackups
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        val payload = backupRestorer.snapshot()
        if (payload.hasProgress) {
            autoBackups.save(LocalDate.now(), DataExportImport.exportToJson(payload))
        }
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "AutoBackupDaily"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
