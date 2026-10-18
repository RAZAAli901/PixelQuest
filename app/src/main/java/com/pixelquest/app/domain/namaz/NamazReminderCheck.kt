package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity

/**
 * Whether an alarm that just fired should show anything (docs/namaz-design.md, Reminders). An
 * alarm can outlive the reason it was armed: the tracker or that prayer's reminder turned off, or
 * the prayer marked from the screen first. The debug test reminder always shows.
 */
object NamazReminderCheck {

    fun shouldRemind(kind: NamazAlarm.Kind, prayer: Prayer, settings: NamazSettingsEntity, logs: List<NamazLogEntity>): Boolean {
        if (kind == NamazAlarm.Kind.TEST) return true
        if (!settings.enabled || !settings.remindsFor(prayer)) return false
        return logs.none { it.prayer == prayer && it.status == PrayerStatus.PRAYED }
    }
}

/** Whether [prayer]'s reminder is on. */
fun NamazSettingsEntity.remindsFor(prayer: Prayer): Boolean = when (prayer) {
    Prayer.FAJR -> remindFajr
    Prayer.DHUHR -> remindDhuhr
    Prayer.ASR -> remindAsr
    Prayer.MAGHRIB -> remindMaghrib
    Prayer.ISHA -> remindIsha
}

/** These settings with [prayer]'s reminder turned [on] or off. */
fun NamazSettingsEntity.withReminder(prayer: Prayer, on: Boolean): NamazSettingsEntity = when (prayer) {
    Prayer.FAJR -> copy(remindFajr = on)
    Prayer.DHUHR -> copy(remindDhuhr = on)
    Prayer.ASR -> copy(remindAsr = on)
    Prayer.MAGHRIB -> copy(remindMaghrib = on)
    Prayer.ISHA -> copy(remindIsha = on)
}
