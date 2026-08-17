package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import kotlinx.coroutines.flow.Flow

interface DifficultySettingsRepository {
    fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?>
    suspend fun insertSettings(settings: DifficultySettingsEntity)
    suspend fun updateSettings(settings: DifficultySettingsEntity)
}
