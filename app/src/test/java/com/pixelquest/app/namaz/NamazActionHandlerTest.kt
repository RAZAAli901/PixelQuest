package com.pixelquest.app.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.DefaultPrayerTimeProvider
import com.pixelquest.app.domain.namaz.MarkPrayerUseCase
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.domain.namaz.NextPrayerUseCase
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.namaz.PrayerTimesCalculator
import com.pixelquest.app.testing.FakeNamazNotifier
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.testing.LahoreZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A reminder's buttons, in Lahore on 17 October 2026 (Asr from about 15:50, Maghrib about 17:29). */
class NamazActionHandlerTest {

    private val day = LocalDate.of(2026, 10, 17)
    private val repository = FakeNamazRepository(NamazSettingsEntity(enabled = true))
    private val clock = FixedClock(LocalDateTime.of(day, LocalTime.of(16, 0)))
    private val provider = DefaultPrayerTimeProvider(repository, PrayerTimesCalculator(), clock) { LahoreZone }
    private val notifier = FakeNamazNotifier()
    private val handler = NamazActionHandler(
        notifier,
        MarkPrayerUseCase(provider, repository, NextPrayerUseCase(provider)),
        clock
    ) { LahoreZone }

    private fun showReminder(prayer: Prayer) = runBlocking {
        notifier.show(NamazReminder(NamazAlarm.Kind.REMINDER, day, prayer, provider.timesFor(day).timeOf(prayer), "Lahore"))
    }

    @Test
    fun prayed_marksThePrayer_andTakesTheReminderAway() = runBlocking {
        showReminder(Prayer.ASR)

        handler.handle(NamazAction.PRAYED, day, Prayer.ASR)

        val row = repository.rows.value[day to Prayer.ASR]!!
        assertEquals(PrayerStatus.PRAYED, row.status)
        assertEquals(clock.time.atZone(LahoreZone).toInstant().toEpochMilli(), row.prayedAt)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun prayed_twice_marksItOnce() = runBlocking {
        handler.handle(NamazAction.PRAYED, day, Prayer.ASR)
        val first = repository.rows.value[day to Prayer.ASR]
        clock.time = clock.time.plusMinutes(5)

        handler.handle(NamazAction.PRAYED, day, Prayer.ASR)

        assertEquals(first, repository.rows.value[day to Prayer.ASR])
    }

    @Test
    fun prayed_beforeThePrayerStarts_marksNothing() = runBlocking {
        handler.handle(NamazAction.PRAYED, day, Prayer.MAGHRIB)

        assertNull(repository.statusOf(day, Prayer.MAGHRIB))
    }

    @Test
    fun dismiss_onlyTakesTheReminderAway() = runBlocking {
        showReminder(Prayer.ASR)

        handler.handle(NamazAction.DISMISS, day, Prayer.ASR)

        assertTrue(notifier.shown.isEmpty())
        assertNull(repository.statusOf(day, Prayer.ASR))
    }
}
