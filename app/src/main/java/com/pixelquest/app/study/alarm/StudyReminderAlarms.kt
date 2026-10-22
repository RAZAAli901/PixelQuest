package com.pixelquest.app.study.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pixelquest.app.scheduling.AlarmArming
import com.pixelquest.app.study.domain.ReminderAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Arms and cancels study reminder alarms: AlarmManager in the app, a fake in tests. */
interface StudyReminderAlarms {
    /** Arms [alert] in its slot, replacing what was armed there. */
    fun schedule(alert: ReminderAlert)

    /** Cancels what's armed in [slot] (a [ReminderAlert.slot]), if anything. */
    fun cancel(slot: String)
}

/**
 * Study reminders' alarms through [AlarmArming], the app's one way of arming (exact when allowed,
 * otherwise a 10-minute window). Each slot is its own alarm by its data address, so arming one
 * never replaces another, and a slot is cancelled without knowing what was in it.
 */
@Singleton
class AlarmManagerStudyReminderAlarms @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmScheduler: com.pixelquest.app.scheduling.AlarmScheduler,
) : StudyReminderAlarms {
    constructor(@ApplicationContext context: Context) : this(
        context,
        com.pixelquest.app.scheduling.AndroidAlarmScheduler(context),
    )

    override fun schedule(alert: ReminderAlert) {
        val intent = intentFor(alert.slot)
            .putExtra(EXTRA_REMINDER, alert.reminderId)
            .putExtra(EXTRA_OCCURRENCE, alert.occurrence.start.toInstant().toEpochMilli())
            .putExtra(EXTRA_INDEX, alert.index)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!alarmScheduler.schedule(alert.at.toEpochMilli(), pending)) {
            com.pixelquest.app.util.AppLog.w(TAG, "Could not arm a study reminder")
        }
    }

    override fun cancel(slot: String) {
        val pending = PendingIntent.getBroadcast(context, 0, intentFor(slot), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarmScheduler.cancel(pending)
    }

    private fun intentFor(slot: String): Intent =
        Intent(context, StudyReminderReceiver::class.java)
            .setAction(ACTION_ALERT)
            .setData(Uri.Builder().scheme(SCHEME).authority(AUTHORITY).path(slot).build())

    companion object {
        private const val TAG = "StudyReminderAlarms"
        const val ACTION_ALERT = "com.pixelquest.app.action.STUDY_REMINDER_ALERT"
        private const val SCHEME = "pixelquest"
        private const val AUTHORITY = "study-reminder"
        const val EXTRA_REMINDER = "com.pixelquest.app.extra.STUDY_REMINDER"
        const val EXTRA_OCCURRENCE = "com.pixelquest.app.extra.STUDY_REMINDER_OCCURRENCE"
        const val EXTRA_INDEX = "com.pixelquest.app.extra.STUDY_REMINDER_INDEX"
    }
}
