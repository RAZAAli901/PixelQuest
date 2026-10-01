package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.InsightCacheDao
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.repository.InsightCacheRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Step 4: Implementation of [InsightCacheRepository] backed by Room's [InsightCacheDao].
 */
@Singleton
class InsightCacheRepositoryImpl @Inject constructor(
    private val insightCacheDao: InsightCacheDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : InsightCacheRepository {

    override suspend fun saveInsight(response: HabitInsightResponse, dataHash: String) {
        val entity = InsightCacheEntity.fromInsightResponse(response, dataHash)
        insightCacheDao.insertInsight(entity)
    }

    override suspend fun getLatestInsight(): InsightCacheEntity? {
        return insightCacheDao.getLatestInsight()
    }

    override fun observeLatestInsight(): Flow<InsightCacheEntity?> {
        return insightCacheDao.observeLatestInsight()
    }

    override suspend fun isCacheValid(currentDataHash: String, maxAgeMillis: Long): Boolean {
        val latest = insightCacheDao.getLatestInsight() ?: return false
        val now = clock()
        val age = now - latest.generatedAt
        val isNotStale = age in 0..maxAgeMillis
        val isDataMatching = latest.dataHash == currentDataHash
        return isNotStale && isDataMatching
    }

    override suspend fun clearExpired(maxAgeMillis: Long): Int {
        val expiryTimestamp = clock() - maxAgeMillis
        return insightCacheDao.deleteOld(expiryTimestamp)
    }

    override suspend fun clearAll(): Int {
        return insightCacheDao.clearCache()
    }
}
