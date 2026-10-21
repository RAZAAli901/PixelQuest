package com.pixelquest.app.study.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.notification.PqNotifications
import com.pixelquest.app.study.domain.ReminderAlert
import com.pixelquest.app.study.domain.ReminderScope
import com.pixelquest.app.study.domain.StudyReminder
import com.pixelquest.app.ui.navigation.Screen
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

/** Posts a study reminder's alert: Android's notifications in the app, a fake in tests. */
interface StudyReminderNotifier {
    fun show(reminder: StudyReminder, alert: ReminderAlert)
}

/**
 * The alert as a notification: the reminder's title and when it is; tapping it opens the party.
 * On the lock screen its text is hidden (docs/study-reminders.md, Privacy).
 */
@Singleton
class AndroidStudyReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : StudyReminderNotifier {

    override fun show(reminder: StudyReminder, alert: ReminderAlert) {
        val whenText = whenText(reminder, alert)
        val open = PendingIntent.getActivity(
            context,
            notificationId(reminder.id),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ROUTE, Screen.PartiesParty.createRoute(reminder.partyId))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hidden = PqNotifications.builder(context, CHANNEL)
            .setContentTitle(context.getString(R.string.study_reminder_notif_hidden))
            .build()
        val notification = PqNotifications.builder(context, CHANNEL)
            .setContentTitle(reminder.title)
            .setContentText(whenText)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(hidden)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(reminder.id), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted.
        }
    }

    /** "This week", "Thursday 22 October", "Thursday 22 October, 10:00", in the phone's own zone. */
    private fun whenText(reminder: StudyReminder, alert: ReminderAlert): String {
        val start = alert.occurrence.start.withZoneSameInstant(ZoneId.systemDefault())
        val day = start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
        return when (reminder.scope) {
            ReminderScope.WEEK -> context.getString(R.string.study_reminder_notif_week)
            ReminderScope.DAY -> day
            ReminderScope.TIME -> context.getString(R.string.study_reminder_day_time, day, start.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
        }
    }

    companion object {
        /** Until study reminders have their own channel: the study party's. */
        private val CHANNEL = NotificationChannels.Spec.STUDY.id

        private const val NOTIFICATION_BASE = 920_000

        fun notificationId(reminderId: String): Int = NOTIFICATION_BASE + (reminderId.hashCode() and 0xFFFF)
    }
}
