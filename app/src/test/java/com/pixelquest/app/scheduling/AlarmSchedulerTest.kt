package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: AndroidAlarmScheduler
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        scheduler = AndroidAlarmScheduler(context)
    }

    @Test
    fun scheduleAndCancel_updatesScheduledAlarms() {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply { action = "TEST_ACTION" }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            123,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val success = scheduler.schedule(atMillis = 15_000L, pendingIntent = pendingIntent)
        assertTrue(success)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertEquals(15_000L, shadowAlarmManager.scheduledAlarms.first().triggerAtTime)

        scheduler.cancel(pendingIntent)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun scheduleAlarmClock_whenAllowed_setsAlarmClockInfo() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            456,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val clockIntent = PendingIntent.getActivity(
            context,
            789,
            Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val success = scheduler.schedule(
            atMillis = 25_000L,
            pendingIntent = pendingIntent,
            isAlarmClock = true,
            clockInfoIntent = clockIntent,
        )
        assertTrue(success)
        val scheduled = shadowAlarmManager.scheduledAlarms.first()
        assertNotNull(scheduled.alarmClockInfo)
        assertEquals(25_000L, scheduled.triggerAtTime)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }

    @Test
    fun openExactAlarmSettingsIntent_returnsValidIntent() {
        val intent = scheduler.openExactAlarmSettingsIntent()
        assertNotNull(intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
