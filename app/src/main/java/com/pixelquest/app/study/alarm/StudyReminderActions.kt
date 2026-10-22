package com.pixelquest.app.study.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelquest.app.study.data.StudyReminderRepository
import com.pixelquest.app.study.data.local.StudyReminderDao
import com.pixelquest.app.study.data.local.StudyReminderDoneEntity
import com.pixelquest.app.study.domain.ReminderAlert
import com.pixelquest.app.study.domain.ReminderAlerts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The study reminder notification's buttons (docs/study-reminders.md, The notification). */
enum class StudyReminderAction(val action: String) {
    MARK_DONE("com.pixelquest.app.action.STUDY_REMINDER_DONE"),
    SNOOZE("com.pixelquest.app.action.STUDY_REMINDER_SNOOZE");

    companion object {
        fun fromAction(action: String?): StudyReminderAction? = entries.firstOrNull { it.action == action }
    }
}

/**
 * What the buttons do. Mark done stops the rest of that occurrence's alerts on this phone (it isn't
 * shared); Snooze brings the alert back in an hour, in a slot of its own.
 */
@Singleton
class StudyReminderActionHandler(
    private val reminders: StudyReminderRepository,
    private val dao: StudyReminderDao,
    private val scheduler: StudyReminderScheduler,
    private val alarms: StudyReminderAlarms,
    private val notifier: StudyReminderNotifier,
    private val clock: () -> Long
) {
    @Inject
    constructor(
        reminders: StudyReminderRepository,
        dao: StudyReminderDao,
        scheduler: StudyReminderScheduler,
        alarms: StudyReminderAlarms,
        notifier: StudyReminderNotifier
    ) : this(reminders, dao, scheduler, alarms, notifier, System::currentTimeMillis)

    suspend fun handle(action: StudyReminderAction, reminderId: String, occurrenceAt: Long) {
        notifier.cancel(reminderId)
        when (action) {
            StudyReminderAction.MARK_DONE -> {
                dao.markDone(StudyReminderDoneEntity(reminderId, occurrenceAt, clock()))
                alarms.cancel(ReminderAlert.snoozeSlot(reminderId))
                scheduler.rescheduleAll()
            }
            StudyReminderAction.SNOOZE -> {
                val reminder = reminders.allReminders().firstOrNull { it.id == reminderId } ?: return
                val occurrence = ReminderAlerts.alert(reminder, Instant.ofEpochMilli(occurrenceAt), 0)?.occurrence ?: return
                val at = Instant.ofEpochMilli(clock()).plus(SNOOZE)
                alarms.schedule(ReminderAlert(reminder.id, reminder.partyId, occurrence, at, ReminderAlert.SNOOZE_INDEX))
            }
        }
    }

    companion object {
        val SNOOZE: Duration = Duration.ofHours(1)
    }
}

/** Receives the notification's buttons (manifest, not exported) and hands them over off the main thread. */
@AndroidEntryPoint
class StudyReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handler: StudyReminderActionHandler

    override fun onReceive(context: Context, intent: Intent) {
        val action = StudyReminderAction.fromAction(intent.action) ?: return
        val reminderId = com.pixelquest.app.util.IntentExtras.uuid(intent, AlarmManagerStudyReminderAlarms.EXTRA_REMINDER) ?: return
        val occurrenceAt = com.pixelquest.app.util.IntentExtras.nonNegativeLong(intent, AlarmManagerStudyReminderAlarms.EXTRA_OCCURRENCE) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.handle(action, reminderId, occurrenceAt)
            } catch (e: Exception) {
                android.util.Log.e("StudyReminderActions", "Could not do $action", e)
            } finally {
                pending.finish()
            }
        }
    }
}
