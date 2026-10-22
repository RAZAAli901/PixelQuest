package com.pixelquest.app.namaz

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.scheduling.AlarmArming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Arms and cancels Namaz alarms: AlarmManager in the app, a fake in tests. */
interface NamazAlarms {
    fun canScheduleExactAlarms(): Boolean

    /** Arms [alarm], replacing the same alarm if it's already armed. */
    fun schedule(alarm: NamazAlarm)

    /** Cancels one alarm, if it's armed. */
    fun cancel(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer)

    /** Cancels every Namaz alarm that could be armed around [today]. */
    fun cancelAll(today: LocalDate)
}

/**
 * Arms and cancels the Namaz Tracker's alarms, one per prayer and kind (docs/namaz-design.md,
 * Reminders). Exact (`setExactAndAllowWhileIdle`, RTC_WAKEUP) when Android allows it; otherwise
 * within a 10-minute window, as the task reminders do.
 */
@Singleton
class NamazAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : NamazAlarms {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExactAlarms(): Boolean = AlarmArming.canScheduleExact(alarmManager)

    override fun schedule(alarm: NamazAlarm) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            intentFor(alarm.kind, alarm.date, alarm.prayer),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val armed = try {
            if (canScheduleExactAlarms() && alarm.alarmClock) {
                // An alarm clock: exact, wakes the phone, and its time shows as the next alarm.
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarm.atMillis, openNamaz()), pendingIntent)
                true
            } else {
                AlarmArming.arm(alarmManager, alarm.atMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            false
        }
        if (!armed) com.pixelquest.app.util.AppLog.w(TAG, "Could not arm the ${alarm.kind} for ${alarm.prayer} on ${alarm.date}")
    }

    override fun cancel(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NamazAlarm.requestCode(kind, date, prayer),
            intentFor(kind, date, prayer),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Two days back to three ahead: covers the 48-hour window from any moment, a snooze past
     * midnight, and a clock moved a day.
     */
    override fun cancelAll(today: LocalDate) {
        for (offset in -2L..3L) {
            val date = today.plusDays(offset)
            NamazAlarm.Kind.entries.forEach { kind -> Prayer.entries.forEach { cancel(kind, date, it) } }
        }
    }

    /** Where an alarm clock's icon leads when tapped: the Namaz screen. */
    private fun openNamaz(): PendingIntent = PendingIntent.getActivity(
        context,
        SHOW_REQUEST_CODE,
        Intent(context, com.pixelquest.app.MainActivity::class.java)
            .putExtra(com.pixelquest.app.MainActivity.EXTRA_OPEN_ROUTE, com.pixelquest.app.ui.navigation.Screen.Namaz.route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** What an alarm delivers: its kind (the action), prayer day and prayer. */
    internal fun intentFor(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer): Intent =
        Intent(context, NamazAlarmReceiver::class.java)
            .setAction(kind.action)
            .putExtra(EXTRA_DATE, date.toEpochDay())
            .putExtra(EXTRA_PRAYER, prayer.name)

    companion object {
        private const val TAG = "NamazAlarmScheduler"

        private const val SHOW_REQUEST_CODE = 741_000

        /** Shortest window Android 12+ honours for inexact alarms. */
        const val INEXACT_WINDOW_MS = AlarmArming.INEXACT_WINDOW_MS

        const val EXTRA_DATE = "com.pixelquest.app.extra.NAMAZ_DATE"
        const val EXTRA_PRAYER = "com.pixelquest.app.extra.NAMAZ_PRAYER"

        /** The prayer day an alarm's intent is for. */
        fun dateFrom(intent: Intent): LocalDate? =
            com.pixelquest.app.util.IntentExtras.epochDay(intent, EXTRA_DATE)

        /** The prayer an alarm's intent is for. */
        fun prayerFrom(intent: Intent): Prayer? =
            intent.getStringExtra(EXTRA_PRAYER)?.let { name -> Prayer.entries.firstOrNull { it.name == name } }
    }
}
