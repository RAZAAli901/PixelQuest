package com.pixelquest.app.namaz

import android.app.Application
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Namaz channels: a loud one for each prayer, a quiet one for nudges, gone again when off. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazChannelsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun reminders_areHighImportance_andNudgesLow_andSilent() {
        NamazChannels.create(context)

        val reminders = manager.getNotificationChannel(NamazChannels.REMINDERS)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, reminders.importance)
        assertEquals("Prayer reminders", reminders.name)
        assertEquals(NamazChannels.GROUP_ID, reminders.group)

        val nudge = manager.getNotificationChannel(NamazChannels.NUDGE)
        assertEquals(NotificationManager.IMPORTANCE_LOW, nudge.importance)
        assertNull(nudge.sound)
        assertTrue(manager.notificationChannelGroups.any { it.id == NamazChannels.GROUP_ID })
    }

    @Test
    fun deleting_removesEveryNamazChannel_andTheGroup() {
        NamazChannels.create(context)

        NamazChannels.delete(context)

        NamazChannels.all.forEach { assertNull(manager.getNotificationChannel(it)) }
        assertTrue(manager.notificationChannelGroups.none { it.id == NamazChannels.GROUP_ID })
    }

    @Test
    fun theSilentChannel_hasNoSoundOrVibration() {
        NamazChannels.create(context)

        val silent = manager.getNotificationChannel(NamazChannels.REMINDERS_SILENT)
        assertNull(silent.sound)
        assertEquals(false, silent.shouldVibrate())
    }
}
