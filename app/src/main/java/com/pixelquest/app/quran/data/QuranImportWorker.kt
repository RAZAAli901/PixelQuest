package com.pixelquest.app.quran.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Loads the Quran from the app's bundled files into the database (QuranImporter), in the background
 * on the app's first start, so the reader opens offline from then on.
 */
@HiltWorker
class QuranImportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val importer: QuranImporter
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result =
        try {
            // Nothing to do once the phone holds this bundle; a newer one replaces it.
            importer.importIfNeeded()
            Result.success()
        } catch (e: QuranImportException) {
            // The bundle itself is wrong: trying again won't help, and the tests stop such a bundle shipping.
            Result.failure()
        }

    companion object {
        private const val WORK_NAME = "quran_import"

        /** Asks for the Quran to be loaded; while a load is waiting or running, another isn't added. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<QuranImportWorker>().build()
            )
        }
    }
}
