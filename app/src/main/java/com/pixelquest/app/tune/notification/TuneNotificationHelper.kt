package com.pixelquest.app.tune.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import com.pixelquest.app.namaz.NamazChannels
import com.pixelquest.app.notification.NotificationChannels
import com.pixelquest.app.tune.data.TuneCacheManager
import com.pixelquest.app.tune.model.TuneTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TuneNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tuneCacheManager: TuneCacheManager
) {
    private fun defaultChannel(target: TuneTarget): String = when (target) {
        TuneTarget.QUEST -> NotificationChannels.Spec.REMINDERS.id
        TuneTarget.NAMAZ -> NamazChannels.REMINDERS
    }

    /**
     * Resolves the channel ID to use for posting a reminder notification.
     * If a custom tune is selected and cached, ensures a deterministic channel exists with that sound.
     * Otherwise returns the default channel.
     */
    fun resolveChannelId(
        target: TuneTarget,
        selectedTuneId: String?
    ): String {
        if (selectedTuneId.isNullOrBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return defaultChannel(target)
        }

        val cachedUri = tuneCacheManager.getUriForTune(selectedTuneId)
            ?: return defaultChannel(target)

        val prefix = if (target == TuneTarget.QUEST) "pq_reminders_tune_" else NamazChannels.CUSTOM_TUNE_PREFIX
        val channelId = "$prefix$selectedTuneId"
        val channelName = if (target == TuneTarget.QUEST) "Quests Due (Custom Tune)" else "Prayer (Custom Tune)"

        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = if (target == TuneTarget.NAMAZ) NamazChannels.GROUP_ID else "daily"
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(
                        if (target == TuneTarget.NAMAZ) AudioAttributes.USAGE_ALARM
                        else AudioAttributes.USAGE_NOTIFICATION
                    )
                    .build()
                setSound(cachedUri, audioAttributes)
            }
            nm.createNotificationChannel(channel)
        }

        return channelId
    }
}
