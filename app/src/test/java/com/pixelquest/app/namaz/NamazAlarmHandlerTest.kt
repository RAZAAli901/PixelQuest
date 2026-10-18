package com.pixelquest.app.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** A fired alarm shows its reminder only while it's still wanted. */
class NamazAlarmHandlerTest {

    private class RecordingNotifier : NamazNotifier {
        val shown = mutableListOf<NamazReminder>()
        override fun show(reminder: NamazReminder) {
            shown += reminder
        }

        override fun cancel(date: LocalDate, prayer: Prayer) {
            shown.removeAll { it.date == date && it.prayer == prayer }
        }
    }

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true, latitude = 31.5204, longitude = 74.3587, city = "Lahore"))
    private val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), FixedClock()) { LahoreZone }
    private val notifier = RecordingNotifier()
    private val handler = NamazAlarmHandler(repository, provider, notifier)

    @Test
    fun aReminder_showsThePrayer_itsTime_andThePlace() = runBlocking {
        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.MAGHRIB)

        val shown = notifier.shown.single()
        assertEquals(Prayer.MAGHRIB, shown.prayer)
        assertEquals(provider.timesFor(day).maghrib, shown.time)
        assertEquals("Lahore", shown.placeName)
    }

    @Test
    fun nothingShows_whenTheTrackerOrThatReminderIsOff_orItsAlreadyPrayed() = runBlocking {
        repository.saveSettings(repository.getSettings().copy(remindAsr = false))
        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.ASR)

        repository.logPrayer(day, Prayer.ISHA, prayedAtMillis = 0)
        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.ISHA)

        repository.saveSettings(repository.getSettings().copy(enabled = false))
        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.FAJR)
        handler.handle(NamazAlarm.Kind.SNOOZE, day, Prayer.FAJR)

        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun theTestReminder_alwaysShows() = runBlocking {
        repository.saveSettings(repository.getSettings().copy(enabled = false))

        handler.handle(NamazAlarm.Kind.TEST, day, Prayer.FAJR)

        assertEquals(1, notifier.shown.size)
    }
}
