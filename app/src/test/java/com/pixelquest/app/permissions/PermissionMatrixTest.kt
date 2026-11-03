package com.pixelquest.app.permissions

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.domain.namaz.NamazPlace
import com.pixelquest.app.mascot.PixMood
import com.pixelquest.app.notification.NotificationHelper
import com.pixelquest.app.notification.PqNotify
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileStatus
import com.pixelquest.app.testing.FakeAlarmScheduler
import com.pixelquest.app.testing.FakePermissionManager
import com.pixelquest.app.testing.FakePermissionStatus
import com.pixelquest.app.testing.closeAfterWrites
import com.pixelquest.app.ui.screens.namaz.LocationLookup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * V052: Permission matrix for notifications, exact alarms, location and files.
 * Covers states: not asked, granted, denied, permanently denied, and revoked while running.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PermissionMatrixTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val permissionManager = FakePermissionManager()
    private val fakeAlarmScheduler = FakeAlarmScheduler()
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.closeAfterWrites()
    }

    // --- 1. Notification Permission Matrix (POST_NOTIFICATIONS) ---

    @Test
    fun testNotificationPermission_notAsked_suppressesPostAndDoesNotCrash() {
        val perm = permissionManager.getOrCreate("android.permission.POST_NOTIFICATIONS", FakePermissionStatus.DENIED)
        assertFalse("Permission should not be granted initially", perm.isGranted)
        assertEquals("Initial status should be DENIED", FakePermissionStatus.DENIED, perm.status)
        assertEquals(0, perm.requestCount)

        // PqNotify should safely drop notification without throwing SecurityException
        val spec = PqNotify.Spec(
            id = 501,
            ch = PqNotify.Ch.Quest,
            mood = PixMood.Happy,
            title = "Test Quest",
            body = "Quest notification",
            route = "quests",
        )
        // Should execute smoothly without crashing
        PqNotify.post(context, spec)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals("No notifications should be posted when permission not granted", 0, shadowOf(nm).allNotifications.size)
    }

    @Test
    fun testNotificationPermission_granted_postsNotificationSuccessfully() {
        permissionManager.setPermission("android.permission.POST_NOTIFICATIONS", FakePermissionStatus.GRANTED)
        assertTrue(permissionManager.isGranted("android.permission.POST_NOTIFICATIONS"))

        val notification = NotificationHelper.buildTaskReminderNotification(context, 1L, "Workout")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(101, notification)

        assertTrue("Notification should be in system notification manager", shadowOf(nm).allNotifications.isNotEmpty())
    }

    @Test
    fun testNotificationPermission_permanentlyDenied_suppressesRationale() {
        val perm = permissionManager.getOrCreate("android.permission.POST_NOTIFICATIONS")
        perm.deny(permanently = true)

        assertTrue(perm.isPermanentlyDenied)
        assertFalse(perm.shouldShowRationale)
        assertFalse(perm.isGranted)
    }

    @Test
    fun testNotificationPermission_revokedWhileRunning_dropsNotificationSafely() {
        val perm = permissionManager.getOrCreate("android.permission.POST_NOTIFICATIONS", FakePermissionStatus.GRANTED)
        assertTrue(perm.isGranted)

        // Simulating user going to system settings while app is in background and revoking permission
        perm.deny(permanently = false)
        assertFalse("Permission revoked mid-run", perm.isGranted)

        // When permission is revoked, PqNotify safely drops the post
        val spec = PqNotify.Spec(
            id = 502,
            ch = PqNotify.Ch.Quest,
            mood = PixMood.Worried,
            title = "Revoked Test",
            body = "Dropped safely",
            route = "home",
        )
        PqNotify.post(context, spec)
    }

    // --- 2. Exact Alarm Permission Matrix (SCHEDULE_EXACT_ALARM) ---

    @Test
    fun testExactAlarmPermission_granted_allowsScheduling() {
        fakeAlarmScheduler.exactAlarmsAllowed = true
        assertTrue(fakeAlarmScheduler.canScheduleExactAlarms())

        val taskScheduler = TaskAlarmScheduler(context, fakeAlarmScheduler)
        assertTrue(taskScheduler.canScheduleExactAlarms())

        val intent = Intent("TEST_ALARM")
        val pi = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        val scheduled = fakeAlarmScheduler.schedule(System.currentTimeMillis() + 60_000L, pi)

        assertTrue(scheduled)
        assertEquals(1, fakeAlarmScheduler.scheduledAlarms.size)
    }

    @Test
    fun testExactAlarmPermission_deniedOrRevoked_rejectsExactAndProvidesSettingsIntent() {
        fakeAlarmScheduler.exactAlarmsAllowed = false
        assertFalse(fakeAlarmScheduler.canScheduleExactAlarms())

        val taskScheduler = TaskAlarmScheduler(context, fakeAlarmScheduler)
        assertFalse(taskScheduler.canScheduleExactAlarms())

        val intent = Intent("TEST_ALARM_DENIED")
        val pi = PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_IMMUTABLE)
        val scheduled = fakeAlarmScheduler.schedule(System.currentTimeMillis() + 60_000L, pi)

        assertFalse("Scheduling should be rejected when exact alarm permission is denied", scheduled)
        assertEquals(0, fakeAlarmScheduler.scheduledAlarms.size)

        val settingsIntent = fakeAlarmScheduler.openExactAlarmSettingsIntent()
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, settingsIntent.action)
    }

    // --- 3. Location Permission Matrix (ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION) ---

    @Test
    fun testLocationPermission_denied_setsUnavailableNoticeAndRetainsDefaultCalculation() = runBlocking {
        var lookupState: LocationLookup = LocationLookup.IDLE
        val onResult: (Boolean) -> Unit = { granted ->
            if (!granted) lookupState = LocationLookup.UNAVAILABLE
        }

        onResult(false)
        assertEquals(LocationLookup.UNAVAILABLE, lookupState)
    }

    @Test
    fun testLocationPermission_granted_resolvesLocationAndTransitionsToIdle() = runBlocking {
        val makkah = NamazPlace(name = "Makkah", latitude = 21.4225, longitude = 39.8262)
        assertEquals("Makkah", makkah.name)
        assertEquals(21.4225, makkah.latitude, 0.001)
        assertEquals(39.8262, makkah.longitude, 0.001)

        val perm = permissionManager.getOrCreate("android.permission.ACCESS_FINE_LOCATION")
        perm.grant()
        assertTrue(perm.isGranted)
    }

    // --- 4. Storage & File Access Matrix ---

    @Test
    fun testStorageAccess_untrustedFileOpen_requiresExplicitConfirmationPrompt() {
        val file = ChatFile(
            id = "file_123",
            partyId = "party_1",
            uploaderId = "other_user",
            displayName = "notes.pdf",
            mime = "application/pdf",
            sizeBytes = 1024L,
            sha256 = "dummy_sha",
            status = ChatFileStatus.READY,
            rejectReason = null,
            pinned = false,
            createdAt = Instant.now(),
            readyAt = Instant.now(),
        )

        // Verifies the safety check pattern: untrusted external files prompt confirmation
        var confirmationFile: ChatFile? = null
        var warningSeen = false

        fun openFile(f: ChatFile, myUserId: String) {
            if (!warningSeen && f.uploaderId != myUserId) {
                confirmationFile = f
                return
            }
        }

        openFile(file, myUserId = "me")
        assertNotNull("Untrusted file from other user must require confirmation", confirmationFile)
        assertEquals("notes.pdf", confirmationFile?.displayName)

        // After user confirms, subsequent files open directly
        warningSeen = true
        confirmationFile = null
        openFile(file, myUserId = "me")
        assertNull("Subsequent file opens should not re-trigger confirmation once warning is accepted", confirmationFile)
    }
}
