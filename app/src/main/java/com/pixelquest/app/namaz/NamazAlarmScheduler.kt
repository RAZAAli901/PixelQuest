package com.pixelquest.app.namaz

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms and cancels the Namaz Tracker's alarms, one per prayer and kind (docs/namaz-design.md,
 * Reminders). Exact (`setExactAndAllowWhileIdle`, RTC_WAKEUP) when Android allows it; otherwise
 * within a 10-minute window, as the task reminders do.
 */
@Singleton
class NamazAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Arms [alarm], replacing the same alarm if it's already armed. */
    fun schedule(alarm: NamazAlarm) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            intentFor(alarm.kind, alarm.date, alarm.prayer),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.atMillis, pendingIntent)
            } else {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, alarm.atMillis, INEXACT_WINDOW_MS, pendingIntent)
            }
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Could not arm the ${alarm.kind} for ${alarm.prayer} on ${alarm.date}", e)
        }
    }

    /** Cancels one alarm, if it's armed. */
    fun cancel(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer) {
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
     * Cancels every Namaz alarm that could be armed around [today]: two days back to three ahead
     * covers the 48-hour window from any moment, a snooze past midnight, and a clock moved a day.
     */
    fun cancelAll(today: LocalDate) {
        for (offset in -2L..3L) {
            val date = today.plusDays(offset)
            NamazAlarm.Kind.entries.forEach { kind -> Prayer.entries.forEach { cancel(kind, date, it) } }
        }
    }

    /** What an alarm delivers: its kind (the action), prayer day and prayer. */
    internal fun intentFor(kind: NamazAlarm.Kind, date: LocalDate, prayer: Prayer): Intent =
        Intent(context, NamazAlarmReceiver::class.java)
            .setAction(kind.action)
            .putExtra(EXTRA_DATE, date.toEpochDay())
            .putExtra(EXTRA_PRAYER, prayer.name)

    companion object {
        private const val TAG = "NamazAlarmScheduler"

        /** Shortest window Android 12+ honours for inexact alarms. */
        const val INEXACT_WINDOW_MS = 10 * 60_000L

        const val EXTRA_DATE = "com.pixelquest.app.extra.NAMAZ_DATE"
        const val EXTRA_PRAYER = "com.pixelquest.app.extra.NAMAZ_PRAYER"

        /** The prayer day an alarm's intent is for. */
        fun dateFrom(intent: Intent): LocalDate? =
            intent.takeIf { it.hasExtra(EXTRA_DATE) }?.let { LocalDate.ofEpochDay(it.getLongExtra(EXTRA_DATE, 0L)) }

        /** The prayer an alarm's intent is for. */
        fun prayerFrom(intent: Intent): Prayer? =
            intent.getStringExtra(EXTRA_PRAYER)?.let { name -> Prayer.entries.firstOrNull { it.name == name } }
    }
}
