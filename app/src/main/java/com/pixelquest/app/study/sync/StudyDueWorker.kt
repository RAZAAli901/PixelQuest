package com.pixelquest.app.study.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.party.data.PartyRepository
import com.pixelquest.app.party.domain.MemberStatus
import com.pixelquest.app.party.domain.PartyType
import com.pixelquest.app.study.data.StudyReminderSettings
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.data.local.StudyDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Which reminder an item gets: a day before it's due, or two hours before. */
enum class DueReminder(val before: Duration) {
    DAY(Duration.ofHours(24)),
    SOON(Duration.ofHours(2)),
    ;

    fun key(itemId: String) = "$itemId:$name"
}

/** An item due soon, as a reminder needs it. */
data class DueItem(val id: String, val partyName: String, val title: String, val dueAt: Instant)

/**
 * The reminders to send at [now] for [items] the player hasn't finished, none sent before ([sent]
 * keys): two hours before for one due within two, else a day before for one due within a day. An
 * item due within two hours that never had its day reminder gets only the closer one.
 */
fun remindersDue(items: List<DueItem>, sent: Set<String>, now: Instant): List<Pair<DueItem, DueReminder>> =
    items.filter { it.dueAt.isAfter(now) }.sortedBy { it.dueAt }.mapNotNull { item ->
        val left = Duration.between(now, item.dueAt)
        when {
            left <= DueReminder.SOON.before -> DueReminder.SOON.takeIf { it.key(item.id) !in sent }
            left <= DueReminder.DAY.before -> DueReminder.DAY.takeIf { it.key(item.id) !in sent }
            else -> null
        }?.let { item to it }
    }

/**
 * Reminders of what's due in the player's study parties (docs/study-party.md, Notifications), on the
 * "Study party" channel: about an hour apart, it brings the parties up to date when it can, then
 * reminds of each unfinished item a day before and two hours before it's due, once each. Off when
 * the player turns reminders off. Tapping one opens the app.
 */
@HiltWorker
class StudyDueWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val parties: PartyRepository,
    private val study: StudyRepository,
    private val dao: StudyDao,
    private val account: DaroodAccount,
    private val settings: StudyReminderSettings,
    private val dueAlarmScheduler: com.pixelquest.app.study.alarm.StudyDueAlarmScheduler? = null,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Reschedule pending 24-hour due alarms when background sync runs (A07)
        dueAlarmScheduler?.rescheduleAll()

        if (!settings.dueReminders.value) return Result.success()
        val me = account.id() ?: return Result.success()
        parties.refreshParties()
        val mine = parties.observeMyParties(PartyType.STUDY).first().filter { it.myStatus == MemberStatus.ACTIVE }
        if (mine.isEmpty()) return Result.success()
        // The server's copy when there's a connection; the phone's otherwise.
        mine.forEach { study.refresh(it.id) }
        val names = mine.associate { it.id to it.name }
        val now = study.now()
        val due = dao.itemsDueBetween(now.toEpochMilli(), now.plus(DueReminder.DAY.before).toEpochMilli())
            .filter { it.partyId in names }
            .filter { dao.completionOf(it.id, me) == null && dao.pendingCompletionOf(it.id) == null }
            .map { DueItem(it.id, names.getValue(it.partyId), it.title, Instant.ofEpochMilli(it.dueAt!!)) }

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sent = prefs.getStringSet(KEY_SENT, emptySet()).orEmpty()
        val toSend = remindersDue(due, sent, now)
        toSend.forEach { (item, reminder) -> notify(item, reminder) }
        // What's remembered: this run's, and those of items still coming, so the set never grows for good.
        val upcoming = due.map { it.id }.toSet()
        val keep = sent.filter { it.substringBefore(':') in upcoming }.toSet() +
            toSend.flatMap { (item, reminder) -> if (reminder == DueReminder.SOON) listOf(DueReminder.SOON.key(item.id), DueReminder.DAY.key(item.id)) else listOf(reminder.key(item.id)) }
        prefs.edit().putStringSet(KEY_SENT, keep).apply()
        return Result.success()
    }

    private fun notify(item: DueItem, reminder: DueReminder) {
        val context = applicationContext
        val title = context.getString(if (reminder == DueReminder.SOON) R.string.study_due_soon_title else R.string.study_due_day_title, item.title)
        val text = context.getString(R.string.study_due_text, item.partyName)
        val openApp = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = PqNotifications.builder(context, NotificationChannels.Spec.STUDY.id)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE + (item.id.hashCode() and 0xFFFF), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }

    companion object {
        private const val WORK_NAME = "StudyDueReminders"
        private const val PREFS = "pixelquest_study"
        private const val KEY_SENT = "due_reminders_sent"
        private const val NOTIFICATION_BASE = 910_000

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudyDueWorker>(1, TimeUnit.HOURS)
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
