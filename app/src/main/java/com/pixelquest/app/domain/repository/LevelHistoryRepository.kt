package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import kotlinx.coroutines.flow.Flow

interface LevelHistoryRepository {
    fun getAllHistory(): Flow<List<LevelHistoryEntity>>
    suspend fun insertLevelHistory(entry: LevelHistoryEntity)
}
