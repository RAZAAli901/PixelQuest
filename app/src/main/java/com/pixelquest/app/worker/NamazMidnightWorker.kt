package com.pixelquest.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.domain.repository.NamazRepository
import com.pixelquest.app.util.AppClock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Just after midnight, starts the new day's tracker: five PENDING rows (NamazRepository.seedDay),
 * so the missed worker has rows to settle and the history shows a day with nothing marked. Rows
 * already there (a prayer marked early, or a second run) are kept. Then the prayer reminders are
 * armed for the next 48 hours from the new day. Does nothing while the tracker is off (the
 * reschedule then only makes sure no alarm is left). Each run queues the next, like StreakEvaluationWorker.
 */
@HiltWorker
class NamazMidnightWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NamazRepository,
    private val clock: AppClock,
    private val reminders: com.pixelquest.app.namaz.NamazReminders
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        if (repository.getSettings().enabled) repository.seedDay(clock.now().toLocalDate())
        reminders.reschedule()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    } finally {
        // WorkManager isn't initialised in unit tests.
        try { scheduleNextRun(applicationContext) } catch (e: IllegalStateException) { }
    }

    companion object {
        /** A minute after midnight, so the phone's date has rolled over. */
        val RUN_AT: LocalTime = LocalTime.of(0, 1)
        private const val WORK_NAME = "NamazMidnightNightly"

        /** Real time until the next [RUN_AT] in the zone of [now], so a clock change doesn't move it. */
        fun delayUntilNextRun(now: ZonedDateTime): Duration {
            val todayAt = now.toLocalDate().atTime(RUN_AT).atZone(now.zone)
            val next = if (now.isBefore(todayAt)) todayAt else now.toLocalDate().plusDays(1).atTime(RUN_AT).atZone(now.zone)
            return Duration.between(now, next)
        }

        private fun request() = OneTimeWorkRequestBuilder<NamazMidnightWorker>()
            .setInitialDelay(delayUntilNextRun(ZonedDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
            .build()

        /** At launch: start the nightly chain unless a run is already waiting. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request())
        }

        /** From a run: queue the next night after this one finishes. */
        fun scheduleNextRun(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request())
        }
    }
}
