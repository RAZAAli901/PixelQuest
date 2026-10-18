package com.pixelquest.app.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazNotifier
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

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true, latitude = 31.5204, longitude = 74.3587, city = "Lahore"))
    private val clock = FixedClock(java.time.LocalDateTime.of(day, java.time.LocalTime.of(20, 0)))
    private val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
    private val notifier = FakeNamazNotifier()
    private val options = com.pixelquest.app.testing.FakeNamazReminderOptionsStore()
    private val handler = NamazAlarmHandler(repository, provider, notifier, options, clock) { LahoreZone }

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

    @Test
    fun anEarlyReminder_saysHowManyMinutesAreLeft() = runBlocking {
        val maghrib = provider.timesFor(day).maghrib
        clock.time = maghrib.minusMinutes(10).toLocalDateTime()

        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.MAGHRIB)
        assertEquals(10, notifier.shown.single().minutesToStart)

        clock.time = maghrib.toLocalDateTime()
        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.MAGHRIB)
        assertEquals(0, notifier.shown.single().minutesToStart)
    }

    @Test
    fun aNudge_carriesTheNextPrayer_andIshasIsTomorrowsFajr() = runBlocking {
        handler.handle(NamazAlarm.Kind.NUDGE, day, Prayer.ASR)
        assertEquals(Prayer.MAGHRIB, notifier.shown.single().next!!.prayer)

        handler.handle(NamazAlarm.Kind.NUDGE, day, Prayer.ISHA)
        val isha = notifier.shown.first { it.prayer == Prayer.ISHA }.next!!
        assertEquals(Prayer.FAJR, isha.prayer)
        assertEquals(day.plusDays(1), isha.day)
    }

    @Test
    fun theChosenSound_goesWithTheReminder() = runBlocking {
        options.update { it.copy(sound = com.pixelquest.app.domain.namaz.NamazSound.SILENT) }

        handler.handle(NamazAlarm.Kind.REMINDER, day, Prayer.ASR)

        assertEquals(com.pixelquest.app.domain.namaz.NamazSound.SILENT, notifier.shown.single().sound)
    }
}
