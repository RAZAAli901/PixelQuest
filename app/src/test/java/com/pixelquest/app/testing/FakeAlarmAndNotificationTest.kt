package com.pixelquest.app.testing

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FakeAlarmAndNotificationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun fakeAlarmScheduler_recordsScheduledAndCancelledAlarms() {
        val scheduler = FakeAlarmScheduler()
        val intent = Intent("com.pixelquest.test.ALARM")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val scheduledTime = 1_700_000_000_000L
        val success = scheduler.schedule(scheduledTime, pendingIntent)

        assertTrue(success)
        assertEquals(1, scheduler.scheduledAlarms.size)
        assertTrue(scheduler.hasScheduledAt(scheduledTime))

        scheduler.cancel(pendingIntent)
        assertEquals(0, scheduler.scheduledAlarms.size)
        assertTrue(scheduler.wasCancelled(pendingIntent))
    }

    @Test
    fun fakeNotificationManager_recordsShownAndCancelledNotifications() {
        val manager = FakeNotificationManager()

        manager.notify(1, "Quest Ready", "Time to complete your daily quest", "reminders")
        assertTrue(manager.isShown(1))
        assertEquals(1, manager.shownNotifications.size)
        assertEquals("Quest Ready", manager.shownNotifications[0].title)

        manager.cancel(1)
        assertFalse(manager.isShown(1))
        assertTrue(manager.wasCancelled(1))
    }
}
