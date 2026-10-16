package com.pixelquest.app.domain.namaz

import androidx.annotation.StringRes
import com.batoulapps.adhan2.CalculationMethod
import com.pixelquest.app.R

/**
 * How Fajr and Isha are worked out (the sun's angle below the horizon, or a fixed interval):
 * the methods the tracker offers, stored by name in NamazSettingsEntity.method.
 */
enum class NamazMethod(@StringRes val displayName: Int, internal val adhan: CalculationMethod) {
    /** University of Islamic Sciences, Karachi: the default (18° and 18°). */
    KARACHI(R.string.namaz_method_karachi, CalculationMethod.KARACHI),
    MUSLIM_WORLD_LEAGUE(R.string.namaz_method_mwl, CalculationMethod.MUSLIM_WORLD_LEAGUE),
    /** Islamic Society of North America (15° and 15°). */
    NORTH_AMERICA(R.string.namaz_method_isna, CalculationMethod.NORTH_AMERICA),
    /** Umm al-Qura, Makkah (Isha 90 minutes after Maghrib). */
    UMM_AL_QURA(R.string.namaz_method_umm_al_qura, CalculationMethod.UMM_AL_QURA),
    EGYPTIAN(R.string.namaz_method_egyptian, CalculationMethod.EGYPTIAN);

    companion object {
        val DEFAULT = KARACHI

        /** A stored name back to a method; anything unknown is the default. */
        fun fromName(name: String?): NamazMethod = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
