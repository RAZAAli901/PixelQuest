package com.pixelquest.app.testing

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNetworkAndPermissionTest {

    @Test
    fun fakeNetworkMonitor_emitsConnectivityChanges() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        assertTrue(monitor.isCurrentlyOnline())

        monitor.isOnline.test {
            assertEquals(true, awaitItem())

            monitor.simulateNetworkDrop()
            assertEquals(false, awaitItem())
            assertFalse(monitor.isCurrentlyOnline())

            monitor.simulateNetworkRestored()
            assertEquals(true, awaitItem())
            assertTrue(monitor.isCurrentlyOnline())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fakePermissionState_handlesTransitionsAndRationale() {
        val perm = FakePermissionState("android.permission.POST_NOTIFICATIONS", FakePermissionStatus.DENIED)
        assertFalse(perm.isGranted)
        assertTrue(perm.shouldShowRationale)
        assertFalse(perm.isPermanentlyDenied)
        assertEquals(0, perm.requestCount)

        perm.launchPermissionRequest(FakePermissionStatus.GRANTED)
        assertEquals(1, perm.requestCount)
        assertTrue(perm.isGranted)
        assertFalse(perm.shouldShowRationale)

        perm.deny(permanently = true)
        assertFalse(perm.isGranted)
        assertFalse(perm.shouldShowRationale)
        assertTrue(perm.isPermanentlyDenied)
    }

    @Test
    fun fakePermissionManager_tracksMultiplePermissions() {
        val manager = FakePermissionManager()
        val notif = "android.permission.POST_NOTIFICATIONS"
        val location = "android.permission.ACCESS_COARSE_LOCATION"

        manager.setPermission(notif, FakePermissionStatus.GRANTED)
        manager.setPermission(location, FakePermissionStatus.DENIED)

        assertTrue(manager.isGranted(notif))
        assertFalse(manager.isGranted(location))

        val locState = manager.getOrCreate(location)
        locState.launchPermissionRequest(FakePermissionStatus.GRANTED)

        assertTrue(manager.isGranted(location))
        assertEquals(1, manager.getRequestCount(location))
    }
}
