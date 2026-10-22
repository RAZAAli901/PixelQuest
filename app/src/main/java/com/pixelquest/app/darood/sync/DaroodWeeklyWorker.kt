package com.pixelquest.app.darood.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodPartyRepository
import com.pixelquest.app.darood.data.DaroodReminderSettings
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.party.domain.MemberStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * The weekly note on the "Darood Party" channel, only for players who turned it on: on how many of
 * the last 7 days they added Darood, across all their parties. Kind whatever the number: it never
 * scolds. Nothing for a player signed out or in no party.
 */
@HiltWorker
class DaroodWeeklyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DaroodPartyRepository,
    private val reminder: DaroodReminderSettings,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!reminder.weeklyReminder.value) return Result.success()
        if (repository.refreshParties() !is SupabaseResult.Success) return Result.success()
        val parties = repository.observeMyParties(com.pixelquest.app.party.domain.PartyType.DAROOD).first().filter { it.myStatus == MemberStatus.ACTIVE }
        if (parties.isEmpty()) return Result.success()

        // The player's days, across their parties: one with an entry in any of them counts.
        val days = mutableSetOf<LocalDate>()
        for (party in parties) {
            val mine = repository.fetchMyDays(party.id)
            if (mine is SupabaseResult.Success) days += mine.data.filterValues { it > 0 }.keys
        }
        val today = repository.today()
        val active = days.count { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) }

        val (title, text) = weeklyNote(applicationContext, active)
        val openApp = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = PqNotifications.builder(applicationContext, NotificationChannels.Spec.DAROOD.id)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 900_011
        private const val WORK_NAME = "DaroodWeeklyReminder"

        /** Fridays at 18:00: Jumu'ah, when Darood is recited most, as the week closes. */
        private val SEND_ON = DayOfWeek.FRIDAY
        private val SEND_AT: LocalTime = LocalTime.of(18, 0)

        /** The note for [activeDays] of the last 7: what the title and text say. */
        fun weeklyNote(context: Context, activeDays: Int): Pair<String, String> {
            val title = context.getString(R.string.darood_weekly_title)
            val text = if (activeDays > 0) {
                context.resources.getQuantityString(R.plurals.darood_weekly_days, activeDays, activeDays)
            } else {
                context.getString(R.string.darood_weekly_none)
            }
            return title to text
        }

        /** Minutes from [now] to the next Friday 18:00 (this one, if it's still to come). */
        fun delayUntilNextRun(now: LocalDateTime): Long {
            val thisWeek = now.toLocalDate().with(TemporalAdjusters.nextOrSame(SEND_ON)).atTime(SEND_AT)
            val next = if (now.isBefore(thisWeek)) thisWeek else thisWeek.plusWeeks(1)
            return Duration.between(now, next).toMinutes()
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DaroodWeeklyWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNextRun(LocalDateTime.now()), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
