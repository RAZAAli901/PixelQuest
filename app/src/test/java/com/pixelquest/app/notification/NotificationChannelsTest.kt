package com.pixelquest.app.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NotificationChannelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Test
    fun channelIds_areUnique() {
        val ids = NotificationChannels.Spec.values().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertFalse(ids.contains(NotificationChannels.LEGACY_CHANNEL_ID))
    }

    @Test
    fun reminderChannel_followsSoundPreference() {
        assertEquals(NotificationChannels.Spec.REMINDERS, NotificationChannels.reminderChannel(soundEnabled = true))
        assertEquals(NotificationChannels.Spec.REMINDERS_SILENT, NotificationChannels.reminderChannel(soundEnabled = false))
    }

    @Test
    fun createAll_registersEveryChannelInTheGroup() {
        NotificationChannels.createAll(context)
        NotificationChannels.Spec.values().forEach { spec ->
            val channel = manager.getNotificationChannel(spec.id)
            assertNotNull("missing ${spec.id}", channel)
            assertEquals(NotificationChannels.GROUP_ID, channel.group)
        }
    }

    @Test
    fun createAll_silentChannelHasNoSoundOrVibration() {
        NotificationChannels.createAll(context)
        val silent = manager.getNotificationChannel(NotificationChannels.Spec.REMINDERS_SILENT.id)
        assertNull(silent.sound)
        assertFalse(silent.shouldVibrate())
        assertEquals(NotificationManager.IMPORTANCE_LOW, silent.importance)

        val loud = manager.getNotificationChannel(NotificationChannels.Spec.REMINDERS.id)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, loud.importance)
        assertTrue(loud.shouldVibrate())
    }

    @Test
    fun createAll_deletesLegacyChannel() {
        manager.createNotificationChannel(
            NotificationChannel(NotificationChannels.LEGACY_CHANNEL_ID, "PixelQuest Reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        NotificationChannels.createAll(context)
        assertNull(manager.getNotificationChannel(NotificationChannels.LEGACY_CHANNEL_ID))
    }

    @Test
    fun systemSettingsIntent_targetsTheChannel() {
        val intent = NotificationChannels.systemSettingsIntent(context, NotificationChannels.Spec.MISSED)
        assertEquals(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(NotificationChannels.Spec.MISSED.id, intent.getStringExtra(android.provider.Settings.EXTRA_CHANNEL_ID))
    }
}
