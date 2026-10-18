package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.NamazDayCount
import com.pixelquest.app.data.local.dao.NamazLogDao
import com.pixelquest.app.data.local.dao.NamazSettingsDao
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import com.pixelquest.app.domain.repository.NamazRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NamazRepositoryImpl @Inject constructor(
    private val logDao: NamazLogDao,
    private val settingsDao: NamazSettingsDao
) : NamazRepository {

    override fun observeSettings(): Flow<NamazSettingsEntity> =
        settingsDao.observe().map { it ?: NamazSettingsEntity() }

    override suspend fun getSettings(): NamazSettingsEntity = settingsDao.get() ?: NamazSettingsEntity()

    override suspend fun saveSettings(settings: NamazSettingsEntity) {
        settingsDao.save(settings.copy(id = NamazSettingsEntity.SINGLE_ROW_ID))
    }

    override fun observeDay(date: LocalDate): Flow<List<NamazLogEntity>> = logDao.observeForDate(date)

    override suspend fun getDay(date: LocalDate): List<NamazLogEntity> = logDao.getForDate(date)

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<NamazLogEntity>> = logDao.observeRange(from, to)

    override fun observePrayedPerDay(from: LocalDate, to: LocalDate): Flow<List<NamazDayCount>> =
        logDao.observePrayedPerDay(from, to)

    override suspend fun logPrayer(date: LocalDate, prayer: Prayer, prayedAtMillis: Long, xpAwarded: Int, bonusXp: Int) {
        logDao.upsert(
            NamazLogEntity(
                date = date, prayer = prayer, status = PrayerStatus.PRAYED, prayedAt = prayedAtMillis,
                xpAwarded = xpAwarded, bonusXp = bonusXp
            )
        )
    }

    override suspend fun undoPrayer(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        logDao.upsert(NamazLogEntity(date = date, prayer = prayer, status = status))
    }

    override suspend fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        val current = logDao.get(date, prayer)
        logDao.upsert(
            current?.copy(id = 0, status = status) ?: NamazLogEntity(date = date, prayer = prayer, status = status)
        )
    }

    override suspend fun seedDay(date: LocalDate) {
        Prayer.entries.forEach { prayer ->
            logDao.insertIfAbsent(NamazLogEntity(date = date, prayer = prayer, status = PrayerStatus.PENDING))
        }
    }
}
