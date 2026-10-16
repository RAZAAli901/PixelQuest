package com.pixelquest.app.domain.namaz

import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.testing.FakeNamazRepository
import com.pixelquest.app.testing.FixedClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Daylight saving changes, the phone's time zone, and what comes after Isha. */
class PrayerTimesEdgeCasesTest {

    private val calculator = PrayerTimesCalculator()

    // New York, by ISNA: the US clocks went forward an hour at 2am on Sunday 8 March 2026.
    private val newYork = ZoneId.of("America/New_York")
    private val isna = PrayerCalcSettings(method = NamazMethod.NORTH_AMERICA, madhab = AsrMadhab.SHAFI)
    private fun nyc(date: LocalDate) = calculator.calculate(date, 40.7128, -74.0060, newYork, isna)

    @Test
    fun theDayTheClocksGoForward_shiftsTheClockTimesByAnHour() {
        val before = nyc(LocalDate.of(2026, 3, 7))
        val after = nyc(LocalDate.of(2026, 3, 8))

        assertEquals(ZoneOffset.ofHours(-5), before.dhuhr.offset)
        assertEquals(ZoneOffset.ofHours(-4), after.dhuhr.offset)
        // The sun barely moves in a day, so the clock time jumps by the hour the clocks did.
        val clockShift = Duration.between(before.dhuhr.toLocalTime(), after.dhuhr.toLocalTime()).toMinutes()
        assertTrue("Dhuhr's clock time moved $clockShift minutes", clockShift in 58..62)
        // Fajr on the change day is after 2am, so it's already on the new time.
        assertEquals(ZoneOffset.ofHours(-4), after.fajr.offset)
    }

    @Test
    fun theDayTheClocksGoForward_staysInOrder_andAboutADayApart() {
        val before = nyc(LocalDate.of(2026, 3, 7))
        val after = nyc(LocalDate.of(2026, 3, 8))
        val order = listOf(after.fajr, after.sunrise, after.dhuhr, after.asr, after.maghrib, after.isha)
        assertEquals(order, order.sortedBy { it.toInstant() })
        // In real time, one day's Dhuhr to the next is still about 24 hours.
        val realGap = Duration.between(before.dhuhr.toInstant(), after.dhuhr.toInstant()).toMinutes()
        assertTrue(realGap in (24 * 60 - 3)..(24 * 60 + 3))
    }

    @Test
    fun thePhonesTimeZone_changesTheClockTimes_notTheMoments() {
        val day = LocalDate.of(2026, 10, 17)
        val lahore = NamazPlace.LAHORE
        val inPakistan = calculator.calculate(day, lahore.latitude, lahore.longitude, ZoneId.of("Asia/Karachi"))
        val onAPhoneSetToUtc = calculator.calculate(day, lahore.latitude, lahore.longitude, ZoneOffset.UTC)

        assertEquals(inPakistan.maghrib.toInstant(), onAPhoneSetToUtc.maghrib.toInstant())
        assertEquals(5, Duration.between(onAPhoneSetToUtc.maghrib.toLocalTime(), inPakistan.maghrib.toLocalTime()).toHours())
    }

    @Test
    fun afterIsha_theNextPrayerIsTomorrowsFajr() = runBlocking {
        val repository = FakeNamazRepository(NamazSettingsEntity(latitude = 31.5204, longitude = 74.3587, city = "Lahore"))
        // 23:30 in Lahore: Isha has started, and tomorrow's Fajr is the next prayer.
        val clock = FixedClock(LocalDateTime.of(2026, 10, 17, 23, 30))
        val provider = DefaultPrayerTimeProvider(repository, calculator, clock) { ZoneId.of("Asia/Karachi") }

        val (today, tomorrow) = provider.todayAndTomorrow()

        assertEquals(LocalDate.of(2026, 10, 17), today.date)
        assertEquals(LocalDate.of(2026, 10, 18), tomorrow.date)
        assertTrue(tomorrow.fajr.isAfter(today.isha))
        assertTrue("tomorrow's Fajr is early the next morning", tomorrow.fajr.toLocalDate() == LocalDate.of(2026, 10, 18) && tomorrow.fajr.hour < 6)
    }

    @Test
    fun withoutAPlace_theTimesAreLahores() = runBlocking {
        val provider = DefaultPrayerTimeProvider(FakeNamazRepository(), calculator, FixedClock()) { ZoneId.of("Asia/Karachi") }
        val day = LocalDate.of(2026, 10, 17)

        val times = provider.timesFor(day)

        assertEquals(calculator.calculate(day, 31.5204, 74.3587, ZoneId.of("Asia/Karachi")), times)
        assertEquals(NamazPlace.LAHORE, DefaultPrayerTimeProvider.placeOf(null, 74.0, null))
        assertEquals(NamazPlace.LAHORE, DefaultPrayerTimeProvider.placeOf(95.0, 74.0, "Nowhere"))
    }
}
