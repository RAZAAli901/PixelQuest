package com.pixelquest.app.testing

import com.pixelquest.app.data.local.dao.NamazDayCount
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.repository.NamazRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** An in-memory NamazRepository: one row per prayer per day, like the real table's unique index. */
open class FakeNamazRepository(
    settings: NamazSettingsEntity = NamazSettingsEntity()
) : NamazRepository {

    val settings = MutableStateFlow(settings)

    /** Keyed by (date, prayer). */
    val rows = MutableStateFlow<Map<Pair<LocalDate, Prayer>, NamazLogEntity>>(emptyMap())

    private fun put(row: NamazLogEntity) {
        rows.value = rows.value + ((row.date to row.prayer) to row)
    }

    override fun observeSettings(): Flow<NamazSettingsEntity> = settings

    override suspend fun getSettings(): NamazSettingsEntity = settings.value

    override suspend fun saveSettings(settings: NamazSettingsEntity) {
        this.settings.value = settings
    }

    override fun observeDay(date: LocalDate): Flow<List<NamazLogEntity>> =
        rows.map { all -> all.values.filter { it.date == date } }

    override suspend fun getDay(date: LocalDate): List<NamazLogEntity> = rows.value.values.filter { it.date == date }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<NamazLogEntity>> =
        rows.map { all -> all.values.filter { it.date in from..to }.sortedBy { it.date } }

    override fun observePrayedPerDay(from: LocalDate, to: LocalDate): Flow<List<NamazDayCount>> =
        rows.map { all ->
            all.values.filter { it.date in from..to }.groupBy { it.date }.toSortedMap()
                .map { (date, day) -> NamazDayCount(date, day.count { it.status == PrayerStatus.PRAYED }) }
        }

    override suspend fun logPrayer(date: LocalDate, prayer: Prayer, prayedAtMillis: Long, xpAwarded: Int) {
        put(NamazLogEntity(date = date, prayer = prayer, status = PrayerStatus.PRAYED, prayedAt = prayedAtMillis, xpAwarded = xpAwarded))
    }

    override suspend fun undoPrayer(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        put(NamazLogEntity(date = date, prayer = prayer, status = status))
    }

    override suspend fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        val current = rows.value[date to prayer]
        put(current?.copy(status = status) ?: NamazLogEntity(date = date, prayer = prayer, status = status))
    }

    override suspend fun seedDay(date: LocalDate) {
        Prayer.entries.forEach { prayer ->
            if ((date to prayer) !in rows.value) put(NamazLogEntity(date = date, prayer = prayer, status = PrayerStatus.PENDING))
        }
    }

    fun statusOf(date: LocalDate, prayer: Prayer): PrayerStatus? = rows.value[date to prayer]?.status
}
