package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.dao.NamazDayCount
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The Namaz Tracker's data: its settings and the prayers logged per prayer day (docs/namaz-design.md). */
interface NamazRepository {

    /** The settings as they change; the defaults until the player first saves any. */
    fun observeSettings(): Flow<NamazSettingsEntity>

    suspend fun getSettings(): NamazSettingsEntity

    suspend fun saveSettings(settings: NamazSettingsEntity)

    /** [date]'s prayers that have a row (a day starts with five pending ones; see [seedDay]). */
    fun observeDay(date: LocalDate): Flow<List<NamazLogEntity>>

    suspend fun getDay(date: LocalDate): List<NamazLogEntity>

    /** [from] to [to], inclusive. */
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<NamazLogEntity>>

    /** How many prayers were prayed on each day from [from] to [to] that has rows. */
    fun observePrayedPerDay(from: LocalDate, to: LocalDate): Flow<List<NamazDayCount>>

    /** Marks [prayer] on [date] prayed at [prayedAtMillis], with its own XP and the bonuses it unlocked. */
    suspend fun logPrayer(date: LocalDate, prayer: Prayer, prayedAtMillis: Long, xpAwarded: Int = 0, bonusXp: Int = 0)

    /** Takes a prayed mark back: the prayer goes back to [status] (pending, or missed if its window passed). */
    suspend fun undoPrayer(date: LocalDate, prayer: Prayer, status: PrayerStatus)

    /** Sets [prayer]'s status on [date] (the missed worker), keeping when it was prayed and its XP. */
    suspend fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus)

    /** Adds a pending row for each of [date]'s five prayers that doesn't have one yet. */
    suspend fun seedDay(date: LocalDate)
}
