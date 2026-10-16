package com.pixelquest.app.domain.namaz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Prayer times for Lahore (31.5204, 74.3587) by the tracker's defaults, the Karachi method and
 * Hanafi Asr, against an independent calculation: the AlAdhan API (method 1, school 1) for the
 * same place and dates, fetched on 17 Oct 2026. They may differ by a minute in rounding.
 */
class PrayerTimesCalculatorTest {

    private val calculator = PrayerTimesCalculator()
    private val lahore = NamazPlace.LAHORE
    private val pakistan = ZoneId.of("Asia/Karachi")

    private fun times(date: LocalDate, settings: PrayerCalcSettings = PrayerCalcSettings()) =
        calculator.calculate(date, lahore.latitude, lahore.longitude, pakistan, settings)

    private fun assertNear(label: String, expected: String, actual: ZonedDateTime) {
        val minutes = Duration.between(actual.toLocalTime(), LocalTime.parse(expected)).toMinutes()
        assertTrue("$label: expected about $expected, got ${actual.toLocalTime()}", kotlin.math.abs(minutes) <= 2)
    }

    private fun assertLahore(date: LocalDate, fajr: String, sunrise: String, dhuhr: String, asr: String, maghrib: String, isha: String) {
        val t = times(date)
        assertNear("$date Fajr", fajr, t.fajr)
        assertNear("$date sunrise", sunrise, t.sunrise)
        assertNear("$date Dhuhr", dhuhr, t.dhuhr)
        assertNear("$date Asr", asr, t.asr)
        assertNear("$date Maghrib", maghrib, t.maghrib)
        assertNear("$date Isha", isha, t.isha)
        assertEquals(pakistan, t.fajr.zone)
    }

    @Test
    fun lahore_inWinter() = assertLahore(LocalDate.of(2026, 1, 15), "05:37", "07:02", "12:12", "15:44", "17:22", "18:47")

    @Test
    fun lahore_atMidsummer() = assertLahore(LocalDate.of(2026, 6, 21), "03:19", "04:58", "12:04", "17:01", "19:11", "20:50")

    @Test
    fun lahore_inAutumn() = assertLahore(LocalDate.of(2026, 10, 17), "04:46", "06:07", "11:48", "15:50", "17:29", "18:50")

    @Test
    fun thePrayersComeInOrder() {
        val t = times(LocalDate.of(2026, 10, 17))
        val order = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
        assertEquals(order, order.sortedBy { it.toInstant() })
        assertEquals(Prayer.entries.map { t.timeOf(it) }, listOf(t.fajr, t.dhuhr, t.asr, t.maghrib, t.isha))
    }

    @Test
    fun shafiAsr_isEarlierThanHanafi() {
        val day = LocalDate.of(2026, 10, 17)
        val hanafi = times(day).asr
        val shafi = times(day, PrayerCalcSettings(madhab = AsrMadhab.SHAFI)).asr
        assertTrue("one shadow length comes before two", shafi.isBefore(hanafi))
    }

    @Test
    fun isna_hasALaterFajr_thanKarachi() {
        val day = LocalDate.of(2026, 10, 17)
        // 15 degrees below the horizon is reached later in the morning than 18.
        assertTrue(times(day, PrayerCalcSettings(method = NamazMethod.NORTH_AMERICA)).fajr.isAfter(times(day).fajr))
    }

    @Test
    fun ummAlQura_putsIsha90MinutesAfterMaghrib() {
        val t = times(LocalDate.of(2026, 10, 17), PrayerCalcSettings(method = NamazMethod.UMM_AL_QURA))
        assertEquals(90, Duration.between(t.maghrib, t.isha).toMinutes())
    }

    @Test
    fun adjustments_moveTheirPrayerOnly() {
        val day = LocalDate.of(2026, 10, 17)
        val plain = times(day)
        val adjusted = times(day, PrayerCalcSettings(adjustments = mapOf(Prayer.FAJR to 5, Prayer.ISHA to -3)))
        assertEquals(5, Duration.between(plain.fajr, adjusted.fajr).toMinutes())
        assertEquals(-3, Duration.between(plain.isha, adjusted.isha).toMinutes())
        assertEquals(plain.dhuhr, adjusted.dhuhr)
    }

    @Test
    fun theSettingsRow_isReadIntoTheCalculation() {
        val row = com.pixelquest.app.data.local.entity.NamazSettingsEntity(method = "EGYPTIAN", asrMadhab = "SHAFI", highLatitudeRule = "SEVENTH_OF_THE_NIGHT", adjustDhuhr = 2)
        val settings = row.toCalcSettings()
        assertEquals(NamazMethod.EGYPTIAN, settings.method)
        assertEquals(AsrMadhab.SHAFI, settings.madhab)
        assertEquals(NamazHighLatitudeRule.SEVENTH_OF_THE_NIGHT, settings.highLatitudeRule)
        assertEquals(2, settings.adjustments[Prayer.DHUHR])
        // Unknown names fall back to the defaults rather than failing.
        val odd = com.pixelquest.app.data.local.entity.NamazSettingsEntity(method = "SOMETHING", asrMadhab = "", highLatitudeRule = "x").toCalcSettings()
        assertEquals(PrayerCalcSettings(adjustments = odd.adjustments), odd)
    }
}
