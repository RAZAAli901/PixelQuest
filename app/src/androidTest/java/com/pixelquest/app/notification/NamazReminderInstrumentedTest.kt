package com.pixelquest.app.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.data.repository.NamazRepositoryImpl
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.namaz.AndroidNamazNotifier
import com.pixelquest.app.namaz.NamazAction
import com.pixelquest.app.namaz.NamazActionHandler
import com.pixelquest.app.namaz.NamazAlarmHandler
import com.pixelquest.app.namaz.NamazChannels
import com.pixelquest.app.namaz.PrefsNamazDeliveries
import com.pixelquest.app.testing.FakeNamazAlarms
import com.pixelquest.app.testing.FakeNamazDeliveries
import com.pixelquest.app.testing.FakeNamazReminderOptionsStore
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * On a device: a fired prayer alarm posts the right notification, and its Prayed button marks the
 * prayer. The receivers' own handlers run with Android's real NotificationManager, around an
 * in-memory database and a clock fixed at 16:00 on 17 October 2026 in Lahore (Asr). The app's own
 * data on the device is never touched.
 *
 * Run with: adb shell am instrument -w -e class com.pixelquest.app.notification.NamazReminderInstrumentedTest
 *   com.pixelquest.app.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class NamazReminderInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val system = context.getSystemService(NotificationManager::class.java)
    private val day = LocalDate.of(2026, 10, 17)
    private lateinit var db: AppDatabase
    private lateinit var repository: NamazRepositoryImpl
    private val clock = FixedClock(LocalDateTime.of(day, LocalTime.of(16, 0)))
    private val notifier = AndroidNamazNotifier(context)

    @Before
    fun setUp() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        // The app's own launch reschedules too, and with its tracker off it clears the Namaz channels
        // once (NamazReminders). Let that finish first, so it can't take away the reminder shown here.
        val appDeliveries = PrefsNamazDeliveries(context)
        repeat(60) { if (!appDeliveries.cleared) Thread.sleep(50) }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = NamazRepositoryImpl(db.namazLogDao(), db.namazSettingsDao())
        repository.saveSettings(NamazSettingsEntity(enabled = true, latitude = 31.5204, longitude = 74.3587, city = "Lahore"))
    }

    @After
    fun tearDown() {
        notifier.cancel(day, Prayer.ASR)
        db.close()
    }

    private fun posted(): Notification? = system.activeNotifications
        .firstOrNull { it.id == NamazAlarm.requestCode(NamazAlarm.Kind.REMINDER, day, Prayer.ASR) }
        ?.notification

    /** Posting and cancelling go through the system, so they show in activeNotifications a moment later. */
    private fun waitFor(condition: () -> Boolean) {
        repeat(40) {
            if (condition()) return
            Thread.sleep(50)
        }
    }

    @Test
    fun theAlarm_postsTheReminder_andPrayedMarksThePrayer() = runBlocking {
        assumeTrue("notifications must be allowed for this app", system.areNotificationsEnabled())
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
        val alarmHandler = NamazAlarmHandler(
            repository, provider, notifier, FakeNamazReminderOptionsStore(), FakeNamazDeliveries(), clock
        ) { LahoreZone }
        val actionHandler = NamazActionHandler(
            notifier,
            MarkPrayerUseCase(provider, repository, NextPrayerUseCase(provider), com.pixelquest.app.testing.FakeUserProfileRepository()),
            FakeNamazAlarms(),
            clock
        ) { LahoreZone }

        // The alarm fires: Asr's reminder appears.
        alarmHandler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.ASR)
        waitFor { posted() != null }
        val notification = posted()
        assertNotNull("the reminder is showing", notification)
        assertEquals("Time for Asr", notification!!.extras.getString(Notification.EXTRA_TITLE))
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        assertEquals(true, text.endsWith("· Lahore"))
        assertEquals(listOf("Prayed", "Snooze 10 min", "Dismiss"), notification.actions.map { it.title.toString() })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) assertEquals(NamazChannels.REMINDERS, notification.channelId)

        // Prayed: the prayer is marked, and the reminder goes.
        actionHandler.handle(NamazAction.PRAYED, day, Prayer.ASR)
        assertEquals(PrayerStatus.PRAYED, repository.getDay(day).single { it.prayer == Prayer.ASR }.status)
        waitFor { posted() == null }
        assertNull("the reminder is gone", posted())
    }
}
