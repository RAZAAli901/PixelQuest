package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.DifficultySettingsDao
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DifficultySettingsRepositoryImpl @Inject constructor(
    private val difficultySettingsDao: DifficultySettingsDao
) : DifficultySettingsRepository {
    override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = difficultySettingsDao.getCurrentDifficulty()
    override suspend fun insertSettings(settings: DifficultySettingsEntity) = difficultySettingsDao.insertSettings(settings)
    override suspend fun updateSettings(settings: DifficultySettingsEntity) = difficultySettingsDao.updateSettings(settings)
}
