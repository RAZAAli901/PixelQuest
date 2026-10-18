package com.pixelquest.app.namaz

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
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

    /** The reminders with Silent chosen: in the shade without sound or vibration. */
    const val REMINDERS_SILENT = "namaz_reminders_silent"

    /** "Still not prayed" before the next prayer: low importance, no sound. */
    const val NUDGE = "namaz_nudge"

    /** Every channel this object has ever created, for deleting them all. */
    val all: List<String> get() = listOf(REMINDERS, REMINDERS_SILENT, NUDGE)

    /** The channel a prayer's reminder goes to for [sound]: sound is fixed per channel on Android 8+. */
    fun reminderChannel(sound: NamazSound): String = when (sound) {
        NamazSound.SYSTEM -> REMINDERS
        NamazSound.SILENT -> REMINDERS_SILENT
    }

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
                    REMINDERS_SILENT,
                    context.getString(R.string.namaz_channel_reminders_silent),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.namaz_channel_reminders_silent_description)
                    group = GROUP_ID
                    setSound(null, null)
                    enableVibration(false)
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
