package com.pixelquest.app.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Every notification channel PixelQuest posts to. On Android 8+ sound and vibration are fixed per
 * channel once created, so a "sound off" reminder goes to the silent channel instead of trying to
 * mute a single notification (which Android ignores).
 */
object NotificationChannels {

    const val GROUP_ID = "pq_group"
    const val GROUP_NAME = "PixelQuest"

    /** Channel id used before Day 26; deleted on startup so it no longer shows in system settings. */
    const val LEGACY_CHANNEL_ID = "pixelquest_reminders_channel"

    enum class Importance { HIGH, DEFAULT, LOW }

    enum class Spec(
        val id: String,
        val displayName: String,
        val description: String,
        val importance: Importance,
        val sound: Boolean,
        val vibration: Boolean
    ) {
        REMINDERS(
            id = "pq_reminders",
            displayName = "Task reminders",
            description = "Alerts at the time you set for each task",
            importance = Importance.HIGH,
            sound = true,
            vibration = true
        ),
        REMINDERS_SILENT(
            id = "pq_reminders_silent",
            displayName = "Silent reminders",
            description = "Task reminders that appear without sound or vibration",
            importance = Importance.LOW,
            sound = false,
            vibration = false
        ),
        MISSED(
            id = "pq_missed",
            displayName = "Missed tasks",
            description = "Tasks still open two hours after their time",
            importance = Importance.DEFAULT,
            sound = false,
            vibration = true
        ),
        PROGRESS(
            id = "pq_progress",
            displayName = "Streaks and progress",
            description = "Streak-at-risk nudges and progress updates",
            importance = Importance.DEFAULT,
            sound = false,
            vibration = false
        ),
        COACH(
            id = "pq_coach",
            displayName = "AI Coach",
            description = "Lets you know when a new AI Coach insight is ready (only if AI Coach is on)",
            importance = Importance.LOW,
            sound = false,
            vibration = false
        );
    }

    /** Reminder channel for the user's sound preference. */
    fun reminderChannel(soundEnabled: Boolean): Spec =
        if (soundEnabled) Spec.REMINDERS else Spec.REMINDERS_SILENT

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannelGroup(NotificationChannelGroup(GROUP_ID, GROUP_NAME))
        manager.createNotificationChannels(Spec.values().map { it.toChannel() })
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
    }

    private fun Spec.toChannel(): NotificationChannel {
        val androidImportance = when (importance) {
            Importance.HIGH -> NotificationManager.IMPORTANCE_HIGH
            Importance.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
            Importance.LOW -> NotificationManager.IMPORTANCE_LOW
        }
        return NotificationChannel(id, displayName, androidImportance).apply {
            description = this@toChannel.description
            group = GROUP_ID
            enableVibration(this@toChannel.vibration)
            if (!this@toChannel.sound) setSound(null, null)
        }
    }
}
