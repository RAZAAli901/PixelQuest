package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified alarm scheduler abstraction used across tasks, Namaz, and study reminders (Q040).
 */
interface AlarmScheduler {
    fun canScheduleExactAlarms(): Boolean
    fun schedule(
        atMillis: Long,
        pendingIntent: PendingIntent,
        isAlarmClock: Boolean = false,
        clockInfoIntent: PendingIntent? = null,
    ): Boolean
    fun cancel(pendingIntent: PendingIntent)
    fun openExactAlarmSettingsIntent(): Intent
}

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmScheduler {
    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun canScheduleExactAlarms(): Boolean =
        alarmManager?.let { AlarmArming.canScheduleExact(it) } ?: false

    override fun schedule(
        atMillis: Long,
        pendingIntent: PendingIntent,
        isAlarmClock: Boolean,
        clockInfoIntent: PendingIntent?,
    ): Boolean {
        val manager = alarmManager ?: return false
        return try {
            if (canScheduleExactAlarms() && isAlarmClock && clockInfoIntent != null) {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, clockInfoIntent), pendingIntent)
                true
            } else {
                AlarmArming.arm(manager, atMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            false
        }
    }

    override fun cancel(pendingIntent: PendingIntent) {
        try {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            // Ignored if already canceled
        }
    }

    override fun openExactAlarmSettingsIntent(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
