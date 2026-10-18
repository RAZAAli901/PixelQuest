package com.pixelquest.app.scheduling

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every broadcast after which reminders must be armed again reaches BootReceiver: a reboot, an
 * update, a time-zone or clock change, and the exact-alarm permission changing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BootReceiverActionsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun theManifest_routesEveryReArmBroadcast_toBootReceiver() {
        val expected = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            BootReceiver.ACTION_EXACT_ALARM_PERMISSION_CHANGED
        )
        expected.forEach { action ->
            assertTrue("$action is a re-arm action", action in BootReceiver.REARM_ACTIONS)
            val receivers = context.packageManager.queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
            assertTrue("$action reaches BootReceiver", receivers.any { it.activityInfo.name == BootReceiver::class.java.name })
        }
    }
}
