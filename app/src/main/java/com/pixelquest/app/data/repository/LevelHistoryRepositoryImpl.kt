package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.LevelHistoryDao
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.domain.repository.LevelHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LevelHistoryRepositoryImpl @Inject constructor(
    private val levelHistoryDao: LevelHistoryDao
) : LevelHistoryRepository {

    override fun getAllHistory(): Flow<List<LevelHistoryEntity>> {
        return levelHistoryDao.getAllHistory()
    }

    override suspend fun insertLevelHistory(entry: LevelHistoryEntity) {
        levelHistoryDao.insertLevelHistory(entry)
    }
}
