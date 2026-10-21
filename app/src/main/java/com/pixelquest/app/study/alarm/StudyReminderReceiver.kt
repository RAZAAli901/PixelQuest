package com.pixelquest.app.study.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelquest.app.study.data.StudyReminderRepository
import com.pixelquest.app.study.data.local.StudyReminderDao
import com.pixelquest.app.study.domain.ReminderAlerts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What a study reminder's alarm does when it fires; the receiver only hands it over. */
@Singleton
class StudyReminderAlertHandler(
    private val reminders: StudyReminderRepository,
    private val dao: StudyReminderDao,
    private val scheduler: StudyReminderScheduler,
    private val notifier: StudyReminderNotifier,
    private val clock: () -> Long
) {
    @Inject
    constructor(reminders: StudyReminderRepository, dao: StudyReminderDao, scheduler: StudyReminderScheduler, notifier: StudyReminderNotifier) :
        this(reminders, dao, scheduler, notifier, System::currentTimeMillis)

    /**
     * Shows the alert for [reminderId]'s occurrence starting at [occurrenceAt], if it's still
     * wanted: the reminder still there, that occurrence not marked done, and not already over. Then
     * the next alerts are armed.
     */
    suspend fun handle(reminderId: String, occurrenceAt: Long, index: Int) {
        val reminder = reminders.allReminders().firstOrNull { it.id == reminderId }
        val now = Instant.ofEpochMilli(clock())
        if (index == com.pixelquest.app.study.domain.ReminderAlert.SNOOZE_INDEX) {
            // A snooze comes back as asked, unless the reminder's gone or that occurrence was marked done.
            val snoozed = reminder?.let { ReminderAlerts.alert(it, Instant.ofEpochMilli(occurrenceAt), 0) }
            if (reminder != null && snoozed != null && !dao.isDone(reminderId, occurrenceAt)) {
                notifier.show(reminder, snoozed.copy(at = now, index = com.pixelquest.app.study.domain.ReminderAlert.SNOOZE_INDEX))
            }
            return
        }
        // As the reminder is now: changed since it was armed, its alert may be at another time, or gone.
        val alert = reminder?.let { ReminderAlerts.alert(it, Instant.ofEpochMilli(occurrenceAt), index) }
        val fresh = alert != null && !alert.at.isAfter(now.plus(EARLY)) && !alert.at.isBefore(now.minus(ReminderAlerts.CATCH_UP))
        if (reminder != null && alert != null && fresh && !scheduler.wasFired(alert) && !dao.isDone(reminderId, occurrenceAt)) {
            scheduler.markFired(alert)
            notifier.show(reminder, alert)
        }
        scheduler.rescheduleAll()
    }

    private companion object {
        /** An alarm may come a little early; one far ahead of its alert is a reminder moved later. */
        val EARLY: java.time.Duration = java.time.Duration.ofMinutes(1)
    }
}

/**
 * Receives study reminders' alarms (manifest, not exported) and hands them to
 * [StudyReminderAlertHandler] off the main thread.
 */
@AndroidEntryPoint
class StudyReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handler: StudyReminderAlertHandler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManagerStudyReminderAlarms.ACTION_ALERT) return
        val reminderId = intent.getStringExtra(AlarmManagerStudyReminderAlarms.EXTRA_REMINDER) ?: return
        val occurrenceAt = intent.getLongExtra(AlarmManagerStudyReminderAlarms.EXTRA_OCCURRENCE, -1L).takeIf { it >= 0 } ?: return
        val index = intent.getIntExtra(AlarmManagerStudyReminderAlarms.EXTRA_INDEX, -1).takeIf { it >= 0 } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.handle(reminderId, occurrenceAt, index)
            } catch (e: Exception) {
                android.util.Log.e("StudyReminderReceiver", "Could not show a study reminder", e)
            } finally {
                pending.finish()
            }
        }
    }
}
