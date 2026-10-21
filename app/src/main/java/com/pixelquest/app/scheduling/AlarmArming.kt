package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

/**
 * How every PixelQuest alarm is armed: exact (`setExactAndAllowWhileIdle`, RTC_WAKEUP) when Android
 * allows it, otherwise within a 10-minute window, which Android 12+ honours without the exact-alarm
 * permission. Shared by the Namaz Tracker's alarms and study reminders.
 */
object AlarmArming {
    /** Shortest window Android 12+ honours for inexact alarms. */
    const val INEXACT_WINDOW_MS = 10 * 60_000L

    fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Arms [pendingIntent] for [atMillis]; false if Android refused (the permission taken back meanwhile). */
    fun arm(alarmManager: AlarmManager, atMillis: Long, pendingIntent: PendingIntent): Boolean = try {
        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, atMillis, INEXACT_WINDOW_MS, pendingIntent)
        }
        true
    } catch (e: SecurityException) {
        false
    }
}
