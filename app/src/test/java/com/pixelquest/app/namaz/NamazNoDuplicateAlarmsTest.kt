package com.pixelquest.app.namaz

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NamazReminderPlanner
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazNotifier
import com.pixelquest.app.testing.FakeNamazReminderOptionsStore
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * On Android's own AlarmManager: rescheduling from every trigger at once (launch, boot, the daily
 * worker, an alarm firing) never leaves two alarms for the same reminder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NamazNoDuplicateAlarmsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))

    @Test
    fun reschedulingAgainAndAgain_keepsOneAlarmPerReminder() = runBlocking {
        val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
        val options = FakeNamazReminderOptionsStore()
        val clock = FixedClock(LocalDateTime.of(2026, 10, 17, 16, 0))
        val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
        val reminders = NamazReminders(
            repository, options, NamazReminderPlanner(provider), NamazAlarmScheduler(context),
            FakeNamazNotifier(), PrefsNamazDeliveries(context), FakeSettingsRepository(), clock
        ) { LahoreZone }

        repeat(5) { reminders.reschedule() }
        assertEquals(10, alarmManager.scheduledAlarms.size)

        // The nudge on and Fajr as an alarm: more alarms, still one each.
        options.update { it.copy(nudge = true, fajrThroughDnd = true) }
        repeat(3) { reminders.reschedule() }
        val expected = NamazReminderPlanner(provider).plan(repository.getSettings(), clock.time.atZone(LahoreZone), options.get()).size
        assertEquals(expected, alarmManager.scheduledAlarms.size)

        // A snooze sits beside them; rescheduling again doesn't add to it.
        NamazAlarmScheduler(context).schedule(NamazAlarm(NamazAlarm.Kind.SNOOZE, clock.time.toLocalDate(), Prayer.ASR, 999L))
        repeat(3) { reminders.reschedule() }
        assertEquals(expected + 1, alarmManager.scheduledAlarms.size)
    }
}
