package com.pixelquest.app.domain.namaz

import androidx.annotation.StringRes
import com.batoulapps.adhan2.HighLatitudeRule
import com.pixelquest.app.R
import com.pixelquest.app.data.local.entity.NamazSettingsEntity

/**
 * Far from the equator, the sun may not go deep enough below the horizon in summer for Fajr and
 * Isha's angles; these rules bound them by a share of the night. Stored by name in
 * NamazSettingsEntity.highLatitudeRule; unset, the library picks one for the latitude.
 */
enum class NamazHighLatitudeRule(@StringRes val displayName: Int, internal val adhan: HighLatitudeRule) {
    MIDDLE_OF_THE_NIGHT(R.string.namaz_high_latitude_middle, HighLatitudeRule.MIDDLE_OF_THE_NIGHT),
    SEVENTH_OF_THE_NIGHT(R.string.namaz_high_latitude_seventh, HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
    TWILIGHT_ANGLE(R.string.namaz_high_latitude_angle, HighLatitudeRule.TWILIGHT_ANGLE);

    companion object {
        /** A stored name back to a rule; null (or anything unknown) means the library's choice. */
        fun fromName(name: String?): NamazHighLatitudeRule? = entries.firstOrNull { it.name == name }
    }
}

/** Everything that decides the times besides the place and the date. */
data class PrayerCalcSettings(
    val method: NamazMethod = NamazMethod.DEFAULT,
    val madhab: AsrMadhab = AsrMadhab.DEFAULT,
    /** Null: the library's recommendation for the latitude. */
    val highLatitudeRule: NamazHighLatitudeRule? = null,
    /** Minutes added to each prayer's calculated time (negative is earlier), for local custom. */
    val adjustments: Map<Prayer, Int> = emptyMap()
)

/** The settings row's calculation choices. */
fun NamazSettingsEntity.toCalcSettings(): PrayerCalcSettings = PrayerCalcSettings(
    method = NamazMethod.fromName(method),
    madhab = AsrMadhab.fromName(asrMadhab),
    highLatitudeRule = NamazHighLatitudeRule.fromName(highLatitudeRule),
    adjustments = mapOf(
        Prayer.FAJR to adjustFajr,
        Prayer.DHUHR to adjustDhuhr,
        Prayer.ASR to adjustAsr,
        Prayer.MAGHRIB to adjustMaghrib,
        Prayer.ISHA to adjustIsha
    )
)

/** [prayer]'s minutes added to its calculated time. */
fun NamazSettingsEntity.adjustmentFor(prayer: Prayer): Int = when (prayer) {
    Prayer.FAJR -> adjustFajr
    Prayer.DHUHR -> adjustDhuhr
    Prayer.ASR -> adjustAsr
    Prayer.MAGHRIB -> adjustMaghrib
    Prayer.ISHA -> adjustIsha
}

/** These settings with [prayer]'s adjustment set to [minutes]. */
fun NamazSettingsEntity.withAdjustment(prayer: Prayer, minutes: Int): NamazSettingsEntity = when (prayer) {
    Prayer.FAJR -> copy(adjustFajr = minutes)
    Prayer.DHUHR -> copy(adjustDhuhr = minutes)
    Prayer.ASR -> copy(adjustAsr = minutes)
    Prayer.MAGHRIB -> copy(adjustMaghrib = minutes)
    Prayer.ISHA -> copy(adjustIsha = minutes)
}
