package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AlarmArmingTest {

    private val alarmManager: AlarmManager = mockk(relaxed = true)
    private val pendingIntent: PendingIntent = mockk(relaxed = true)

    @Test
    fun arm_whenExactAllowed_schedulesExactAndAllowWhileIdle() {
        every { alarmManager.canScheduleExactAlarms() } returns true

        val result = AlarmArming.arm(alarmManager, 1000L, pendingIntent)

        assertTrue(result)
        verify {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 1000L, pendingIntent)
        }
    }

    @Test
    fun arm_whenExactDisallowed_fallsBackToWindowAlarm() {
        every { alarmManager.canScheduleExactAlarms() } returns false

        val result = AlarmArming.arm(alarmManager, 2000L, pendingIntent)

        assertTrue(result)
        verify {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, 2000L, AlarmArming.INEXACT_WINDOW_MS, pendingIntent)
        }
    }

    @Test
    fun arm_whenExactThrowsSecurityException_fallsBackToWindowAlarm() {
        every { alarmManager.canScheduleExactAlarms() } returns true
        every {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 3000L, pendingIntent)
        } throws SecurityException("Permission revoked")

        val result = AlarmArming.arm(alarmManager, 3000L, pendingIntent)

        assertTrue(result)
        verify {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, 3000L, AlarmArming.INEXACT_WINDOW_MS, pendingIntent)
        }
    }

    @Test
    fun arm_whenWindowAlsoFails_returnsFalse() {
        every { alarmManager.canScheduleExactAlarms() } returns false
        every {
            alarmManager.setWindow(any(), any(), any(), any())
        } throws RuntimeException("System error")

        val result = AlarmArming.arm(alarmManager, 4000L, pendingIntent)

        assertFalse(result)
    }
}
