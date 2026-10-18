package com.pixelquest.app.namaz

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.pixelquest.app.R
import com.pixelquest.app.domain.namaz.NamazSound

/**
 * The Namaz Tracker's notification channels, in their own group (docs/namaz-design.md, Reminders).
 * They exist only while the tracker is on: created when it's turned on, deleted when it's turned
 * off, so a player who never uses it doesn't see them in the phone's settings.
 */
object NamazChannels {

    const val GROUP_ID = "pq_namaz"

    /** At each prayer's start: high importance, so it pops up, with sound. */
    const val REMINDERS = "namaz_reminders"

    /** The reminders with the bundled chime chosen. */
    const val REMINDERS_CHIME = "namaz_reminders_chime"

    /** The reminders with Silent chosen: in the shade without sound or vibration. */
    const val REMINDERS_SILENT = "namaz_reminders_silent"

    /** Fajr's reminder as an alarm (Do Not Disturb's Alarms exception lets it through), with the phone's sound or the chime. */
    const val FAJR_ALARM = "namaz_fajr_alarm"
    const val FAJR_ALARM_CHIME = "namaz_fajr_alarm_chime"

    /** "Still not prayed" before the next prayer: low importance, no sound. */
    const val NUDGE = "namaz_nudge"

    /** Every channel this object has ever created, for deleting them all. */
    val all: List<String> get() = listOf(REMINDERS, REMINDERS_CHIME, REMINDERS_SILENT, FAJR_ALARM, FAJR_ALARM_CHIME, NUDGE)

    /** The channel a prayer's reminder goes to for [sound]: sound is fixed per channel on Android 8+. */
    fun reminderChannel(sound: NamazSound, asAlarm: Boolean = false): String = when (sound) {
        NamazSound.SYSTEM -> if (asAlarm) FAJR_ALARM else REMINDERS
        NamazSound.CHIME -> if (asAlarm) FAJR_ALARM_CHIME else REMINDERS_CHIME
        NamazSound.SILENT -> REMINDERS_SILENT
    }

    /** Sounds played as an alarm, which is what lets Do Not Disturb's Alarms exception through. */
    private val alarmAudio: AudioAttributes
        get() = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    /** The bundled chime, as a sound a notification can play. */
    fun chimeUri(context: Context): Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.namaz_chime}")

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannelGroup(NotificationChannelGroup(GROUP_ID, context.getString(R.string.namaz_title)))
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(REMINDERS, context.getString(R.string.namaz_channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.namaz_channel_reminders_description)
                    group = GROUP_ID
                    enableVibration(true)
                },
                NotificationChannel(
                    REMINDERS_CHIME,
                    context.getString(R.string.namaz_channel_reminders_chime),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.namaz_channel_reminders_chime_description)
                    group = GROUP_ID
                    enableVibration(true)
                    setSound(
                        chimeUri(context),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                },
                NotificationChannel(
                    REMINDERS_SILENT,
                    context.getString(R.string.namaz_channel_reminders_silent),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.namaz_channel_reminders_silent_description)
                    group = GROUP_ID
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(FAJR_ALARM, context.getString(R.string.namaz_channel_fajr_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.namaz_channel_fajr_alarm_description)
                    group = GROUP_ID
                    enableVibration(true)
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, alarmAudio)
                },
                NotificationChannel(FAJR_ALARM_CHIME, context.getString(R.string.namaz_channel_fajr_alarm_chime), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.namaz_channel_fajr_alarm_description)
                    group = GROUP_ID
                    enableVibration(true)
                    setSound(chimeUri(context), alarmAudio)
                },
                NotificationChannel(NUDGE, context.getString(R.string.namaz_channel_nudge), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.namaz_channel_nudge_description)
                    group = GROUP_ID
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        )
    }

    fun delete(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        all.forEach { manager.deleteNotificationChannel(it) }
        manager.deleteNotificationChannelGroup(GROUP_ID)
    }
}
