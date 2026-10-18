package com.pixelquest.app.namaz

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerMoment
import com.pixelquest.app.domain.namaz.NamazSound
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.screens.namaz.prayerTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A prayer reminder to show: which prayer, on which prayer day, its time, and where.
 *
 * @param minutesToStart how long until the prayer starts, for a reminder set to come early; 0 once it has.
 * @param next for a nudge, the prayer that starts next.
 * @param sound how it sounds (NamazReminderOptions).
 * @param asAlarm comes as an alarm, through Do Not Disturb (Fajr's, when opted in).
 */
data class NamazReminder(
    val kind: NamazAlarm.Kind,
    val date: LocalDate,
    val prayer: Prayer,
    val time: ZonedDateTime,
    val placeName: String,
    val minutesToStart: Int = 0,
    val next: PrayerMoment? = null,
    val sound: NamazSound = NamazSound.SYSTEM,
    val asAlarm: Boolean = false
) {
    /** One notification per prayer and day: a snooze, a nudge or a second alarm replaces it, not stacks. */
    val notificationId: Int get() = NamazAlarm.requestCode(NamazAlarm.Kind.REMINDER, date, prayer)
}

/** Shows Namaz reminders: the system's notifications in the app, a fake in tests. */
interface NamazNotifier {
    fun show(reminder: NamazReminder)

    /** Takes [prayer]'s reminder on [date] off the screen. */
    fun cancel(date: LocalDate, prayer: Prayer)
}

@Singleton
class AndroidNamazNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : NamazNotifier {

    /**
     * "Time for Fajr", then "5:12 AM · Lahore" in the phone's time format (no place: the time
     * alone). Tapping it opens the Namaz screen; its buttons are Prayed, Snooze 10 min and Dismiss.
     * One that comes early says "Fajr in 10 minutes", without Prayed: it can't be marked yet.
     */
    override fun show(reminder: NamazReminder) {
        NamazChannels.create(context)
        val notification = if (reminder.kind == NamazAlarm.Kind.NUDGE) nudge(reminder) else prayerReminder(reminder)
        try {
            NotificationManagerCompat.from(context).notify(reminder.notificationId, notification)
        } catch (e: SecurityException) {
            // Notifications not allowed (Android 13+): nothing to show.
        }
    }

    private val timeFormat get() = prayerTimeFormatter(DateFormat.is24HourFormat(context))

    private fun prayerReminder(reminder: NamazReminder): Notification {
        val name = context.getString(reminder.prayer.displayName)
        val time = reminder.time.format(timeFormat)
        val text = if (reminder.placeName.isBlank()) time else context.getString(R.string.namaz_notification_text, time, reminder.placeName)
        val early = reminder.minutesToStart > 0
        val title = if (early) {
            context.resources.getQuantityString(R.plurals.namaz_notification_title_soon, reminder.minutesToStart, name, reminder.minutesToStart)
        } else {
            context.getString(R.string.namaz_notification_title, name)
        }
        val builder = NotificationCompat.Builder(context, NamazChannels.reminderChannel(reminder.sound, reminder.asAlarm))
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NotificationHelper.NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openNamaz())
        if (!early) builder.addAction(0, context.getString(R.string.namaz_action_prayed), action(NamazAction.PRAYED, reminder))
        return builder
            .addAction(0, context.getString(R.string.namaz_action_snooze), action(NamazAction.SNOOZE, reminder))
            .addAction(0, context.getString(R.string.namaz_action_dismiss), action(NamazAction.DISMISS, reminder))
            // A reminder respects Do Not Disturb; an alarm (Fajr's, opted in) is let through by its Alarms exception.
            .setCategory(if (reminder.asAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            // Before Android 8 the priority sets how it's shown: quiet for Silent.
            .setPriority(if (reminder.sound == NamazSound.SILENT) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setSilent(reminder.sound == NamazSound.SILENT)
            // Before Android 8 there are no channels: the chime is set on the notification.
            .apply { if (reminder.sound == NamazSound.CHIME) setSound(NamazChannels.chimeUri(context)) }
            .setAutoCancel(true)
            .build()
    }

    /** "Still time for Asr", then "Maghrib starts at 5:29 PM": quiet, with Prayed and Dismiss. */
    private fun nudge(reminder: NamazReminder): Notification {
        val name = context.getString(reminder.prayer.displayName)
        val text = reminder.next?.let { context.getString(R.string.namaz_nudge_text, context.getString(it.prayer.displayName), it.time.format(timeFormat)) }
        return NotificationCompat.Builder(context, NamazChannels.NUDGE)
            .setSmallIcon(R.drawable.ic_tasks)
            .setColor(NotificationHelper.NOTIFICATION_ACCENT_COLOR)
            .setContentTitle(context.getString(R.string.namaz_nudge_title, name))
            .setContentText(text)
            .setContentIntent(openNamaz())
            .addAction(0, context.getString(R.string.namaz_action_prayed), action(NamazAction.PRAYED, reminder))
            .addAction(0, context.getString(R.string.namaz_action_dismiss), action(NamazAction.DISMISS, reminder))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
    }

    override fun cancel(date: LocalDate, prayer: Prayer) {
        NotificationManagerCompat.from(context).cancel(NamazAlarm.requestCode(NamazAlarm.Kind.REMINDER, date, prayer))
    }

    /** A button's broadcast to NamazActionReceiver, for this prayer and day. */
    private fun action(action: NamazAction, reminder: NamazReminder): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.requestCode(reminder.date, reminder.prayer),
        Intent(context, NamazActionReceiver::class.java)
            .setAction(action.action)
            .putExtra(NamazAlarmScheduler.EXTRA_DATE, reminder.date.toEpochDay())
            .putExtra(NamazAlarmScheduler.EXTRA_PRAYER, reminder.prayer.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Opens the app on the Namaz screen (MainActivity.EXTRA_OPEN_ROUTE). */
    private fun openNamaz(): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_REQUEST_CODE,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ROUTE, Screen.Namaz.route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        /** Every reminder opens the same screen, so they share one PendingIntent. */
        const val OPEN_REQUEST_CODE = 740_000
    }
}
