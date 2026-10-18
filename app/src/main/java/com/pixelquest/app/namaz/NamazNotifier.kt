package com.pixelquest.app.namaz

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.notification.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** A prayer reminder to show: which prayer, on which prayer day, its time, and where. */
data class NamazReminder(
    val kind: NamazAlarm.Kind,
    val date: LocalDate,
    val prayer: Prayer,
    val time: ZonedDateTime,
    val placeName: String
) {
    /** One notification per prayer and day: a snooze or a second alarm replaces it, not stacks. */
    val notificationId: Int get() = NamazAlarm.requestCode(NamazAlarm.Kind.REMINDER, date, prayer)
}

/** Shows Namaz reminders: the system's notifications in the app, a fake in tests. */
interface NamazNotifier {
    fun show(reminder: NamazReminder)
}

@Singleton
class AndroidNamazNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : NamazNotifier {

    override fun show(reminder: NamazReminder) {
        NamazChannels.create(context)
        val name = context.getString(reminder.prayer.displayName)
        val notification = NotificationCompat.Builder(context, NamazChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NotificationHelper.NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(name)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(reminder.notificationId, notification)
        } catch (e: SecurityException) {
            // Notifications not allowed (Android 13+): nothing to show.
        }
    }
}
