package com.pixelquest.app.darood.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.remote.DaroodAccount
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * The weekly ranking's notification (docs/darood-weekly.md): every few hours, and whenever the app
 * opens, each Darood Party's last week is closed and told once ([WeekResultsRun]). A week ends at
 * Sunday midnight in the party's own zone, so the rounds after it pick it up.
 */
@HiltWorker
class DaroodWeekResultsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DaroodPartyRepository,
    private val account: DaroodAccount,
    private val notifier: WeekResultNotifier,
    private val mutes: WeekRankingMutes,
    private val goalsTold: GoalToldStore,
    private val goalNotifier: GoalNotifier
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        WeekResultsRun(repository, account::id, notifier, mutes).run()
        // Goal reached and New goal set (J089), in the same rounds.
        GoalNotifyRun(repository, account::id, goalsTold, goalNotifier).run()
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "pixelquest_darood_week_results"
        private const val ON_OPEN = "pixelquest_darood_week_results_open"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Every 3 hours, and one round now (the app has just opened). */
        fun schedule(context: Context) {
            val work = WorkManager.getInstance(context)
            work.enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DaroodWeekResultsWorker>(3, TimeUnit.HOURS).setConstraints(online).build()
            )
            work.enqueueUniqueWork(
                ON_OPEN, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<DaroodWeekResultsWorker>().setConstraints(online).build()
            )
        }
    }
}
