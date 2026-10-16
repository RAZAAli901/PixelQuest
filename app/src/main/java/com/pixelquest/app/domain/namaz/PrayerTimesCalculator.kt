package com.pixelquest.app.domain.namaz

import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.toJavaInstant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** One prayer day's times in a time zone: the five prayers, and sunrise (which ends Fajr's window). */
data class PrayerDayTimes(
    val date: LocalDate,
    val fajr: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val dhuhr: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime
) {
    fun timeOf(prayer: Prayer): ZonedDateTime = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}

/**
 * Works out a day's prayer times on the phone with the Adhan library (docs/namaz-design.md), so
 * they need no network.
 */
class PrayerTimesCalculator @Inject constructor() {

    /**
     * [date]'s times at [latitude], [longitude], as clock times in [zone] (the phone's own), by
     * [method] (Karachi unless the player chose another), with Asr by [madhab] (Hanafi by default).
     */
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: NamazMethod = NamazMethod.DEFAULT,
        madhab: AsrMadhab = AsrMadhab.DEFAULT
    ): PrayerDayTimes = calculate(date, latitude, longitude, zone, PrayerCalcSettings(method = method, madhab = madhab))

    /**
     * [date]'s times by every calculation choice in [settings]: the method, the madhab, the
     * high-latitude rule (the library's recommendation when unset) and the minutes added per prayer.
     */
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        settings: PrayerCalcSettings
    ): PrayerDayTimes {
        val parameters = settings.method.adhan.parameters.copy(
            madhab = settings.madhab.adhan,
            highLatitudeRule = settings.highLatitudeRule?.adhan,
            prayerAdjustments = PrayerAdjustments(
                fajr = settings.adjustments[Prayer.FAJR] ?: 0,
                dhuhr = settings.adjustments[Prayer.DHUHR] ?: 0,
                asr = settings.adjustments[Prayer.ASR] ?: 0,
                maghrib = settings.adjustments[Prayer.MAGHRIB] ?: 0,
                isha = settings.adjustments[Prayer.ISHA] ?: 0
            )
        )
        val times = PrayerTimes(
            Coordinates(latitude, longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters
        )
        fun kotlinx.datetime.Instant.inZone(): ZonedDateTime = toJavaInstant().atZone(zone)
        return PrayerDayTimes(
            date = date,
            fajr = times.fajr.inZone(),
            sunrise = times.sunrise.inZone(),
            dhuhr = times.dhuhr.inZone(),
            asr = times.asr.inZone(),
            maghrib = times.maghrib.inZone(),
            isha = times.isha.inZone()
        )
    }
}
