package com.pixelquest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The Namaz Tracker's settings, a single row (docs/namaz-design.md). Every column the later phases
 * need is here from the start, so they don't need another migration. The method, madhab and
 * high-latitude rule are stored by name; the time engine maps them to the prayer-time library's.
 *
 * @param enabled the tracker's on/off switch; off by default, as it's opt-in.
 * @param method a calculation method's name: KARACHI (default), MUSLIM_WORLD_LEAGUE, NORTH_AMERICA,
 *   UMM_AL_QURA or EGYPTIAN.
 * @param asrMadhab HANAFI (default) or SHAFI.
 * @param highLatitudeRule null for the library's recommendation for the latitude.
 * @param latitude the last known place, cached so times work offline; null falls back to Lahore.
 * @param reminderOffsetMin minutes before each prayer to remind (Phase 5).
 * @param adjustFajr minutes added to the calculated time, per prayer, for local custom.
 * @param xpEnabled whether prayers earn XP (Phase 6).
 */
@Entity(tableName = "namaz_settings")
data class NamazSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val enabled: Boolean = false,
    val method: String = "KARACHI",
    val asrMadhab: String = "HANAFI",
    val highLatitudeRule: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val reminderOffsetMin: Int = 0,
    val remindFajr: Boolean = true,
    val remindDhuhr: Boolean = true,
    val remindAsr: Boolean = true,
    val remindMaghrib: Boolean = true,
    val remindIsha: Boolean = true,
    val adjustFajr: Int = 0,
    val adjustDhuhr: Int = 0,
    val adjustAsr: Int = 0,
    val adjustMaghrib: Int = 0,
    val adjustIsha: Int = 0,
    val xpEnabled: Boolean = true
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
