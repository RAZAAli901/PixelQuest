package com.pixelquest.app.study.sync

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
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyReminderSettings
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.ui.boardOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/** One party's line in the weekly summary: the player's points and place this week. */
data class WeekLine(val partyName: String, val points: Int, val place: Int, val members: Int)

/**
 * The weekly summary on the "Study party" channel, only for players who turned it on
 * (docs/study-party.md, Notifications): on Sunday evening, as the week ends, the player's points and
 * place in each study party they're in. Nothing for a player signed out, or in no study party.
 */
@HiltWorker
class StudyWeeklyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val parties: PartyRepository,
    private val study: StudyRepository,
    private val settings: StudyReminderSettings,
    private val appSettings: SettingsRepository? = null,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!settings.weeklySummary.value) return Result.success()
        // Points and places are game terms: Simple Mode has none, so no summary.
        if (appSettings?.simpleModeEnabled?.first() == true) return Result.success()
        val me = study.myId() ?: return Result.success()
        if (parties.refreshParties() !is SupabaseResult.Success) return Result.success()
        val mine = parties.observeMyParties(PartyType.STUDY).first().filter { it.myStatus == MemberStatus.ACTIVE }
        if (mine.isEmpty()) return Result.success()
        val week = study.thisWeek()
        val lines = mine.map { party ->
            parties.refreshParty(party.id)
            study.refresh(party.id)
            val members = parties.observeMembers(party.id).first()
            val board = boardOf(study.observeScores(party.id, week).first(), members, me)
            val line = board.firstOrNull { it.isMe }
            WeekLine(party.name, line?.points ?: 0, line?.place ?: board.size, board.size)
        }
        val (title, text) = summary(applicationContext, lines)
        val openApp = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            Intent(applicationContext, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = PqNotifications.builder(applicationContext, NotificationChannels.Spec.STUDY.id)
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
        const val NOTIFICATION_ID = 900_021
        private const val WORK_NAME = "StudyWeeklySummary"

        /** Sundays at 19:00, as the week ends. */
        private val SEND_ON = DayOfWeek.SUNDAY
        private val SEND_AT: LocalTime = LocalTime.of(19, 0)

        /** The summary's title and text: one line per party ("Physics group: 45 points, 2nd of 5"). */
        fun summary(context: Context, lines: List<WeekLine>): Pair<String, String> {
            val title = context.getString(R.string.study_weekly_title)
            val text = lines.joinToString("\n") { line ->
                context.getString(
                    R.string.study_weekly_line,
                    line.partyName,
                    context.resources.getQuantityString(R.plurals.study_points, line.points, line.points),
                    line.place,
                    line.members,
                )
            }
            return title to text
        }

        /** Minutes from [now] to the next Sunday 19:00 (this one, if it's still to come). */
        fun delayUntilNextRun(now: LocalDateTime): Long {
            val thisWeek = now.toLocalDate().with(TemporalAdjusters.nextOrSame(SEND_ON)).atTime(SEND_AT)
            val next = if (now.isBefore(thisWeek)) thisWeek else thisWeek.plusWeeks(1)
            return Duration.between(now, next).toMinutes()
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudyWeeklyWorker>(7, TimeUnit.DAYS)
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
