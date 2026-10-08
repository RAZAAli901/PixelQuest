package com.pixelquest.app.scheduling

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reminders are armed at absolute times; only boot used to re-arm them, so after flying across time
 * zones an 08:00 reminder rang at the old zone's 08:00. Time zone and clock changes now re-arm too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TimeChangeRearmTest {

    @Test
    fun timeZoneAndClockChanges_reArmReminders() {
        assertTrue(Intent.ACTION_TIMEZONE_CHANGED in BootReceiver.REARM_ACTIONS)
        assertTrue(Intent.ACTION_TIME_CHANGED in BootReceiver.REARM_ACTIONS)
        assertTrue(Intent.ACTION_BOOT_COMPLETED in BootReceiver.REARM_ACTIONS)
    }

    @Test
    fun theManifest_deliversThemToTheReceiver() {
        val packageManager = ApplicationProvider.getApplicationContext<Application>().packageManager
        for (action in BootReceiver.REARM_ACTIONS) {
            val receivers = packageManager.queryBroadcastReceivers(
                Intent(action).setPackage("com.pixelquest.app"), PackageManager.MATCH_ALL
            ).map { it.activityInfo.name }
            assertEquals("$action reaches BootReceiver", listOf(BootReceiver::class.java.name), receivers.filter { it.endsWith("BootReceiver") })
        }
    }
}
