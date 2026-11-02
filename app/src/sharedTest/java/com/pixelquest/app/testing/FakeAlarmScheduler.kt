package com.pixelquest.app.testing

import android.app.PendingIntent
import android.content.Intent
import android.provider.Settings
import com.pixelquest.app.scheduling.AlarmScheduler

/**
 * Deterministic test fake implementing [AlarmScheduler].
 * Records every alarm scheduled or cancelled for test assertions.
 */
class FakeAlarmScheduler : AlarmScheduler {

    data class ScheduledAlarm(
        val atMillis: Long,
        val pendingIntent: PendingIntent,
        val isAlarmClock: Boolean = false,
        val clockInfoIntent: PendingIntent? = null,
    )

    var exactAlarmsAllowed: Boolean = true
    val scheduledAlarms = mutableListOf<ScheduledAlarm>()
    val cancelledAlarms = mutableListOf<PendingIntent>()

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed

    override fun schedule(
        atMillis: Long,
        pendingIntent: PendingIntent,
        isAlarmClock: Boolean,
        clockInfoIntent: PendingIntent?,
    ): Boolean {
        if (!exactAlarmsAllowed) return false
        scheduledAlarms.add(ScheduledAlarm(atMillis, pendingIntent, isAlarmClock, clockInfoIntent))
        return true
    }

    override fun cancel(pendingIntent: PendingIntent) {
        cancelledAlarms.add(pendingIntent)
        scheduledAlarms.removeAll { it.pendingIntent == pendingIntent }
    }

    override fun openExactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)

    fun hasScheduledAt(millis: Long): Boolean =
        scheduledAlarms.any { it.atMillis == millis }

    fun wasCancelled(pendingIntent: PendingIntent): Boolean =
        cancelledAlarms.contains(pendingIntent)

    fun clear() {
        scheduledAlarms.clear()
        cancelledAlarms.clear()
    }
}
