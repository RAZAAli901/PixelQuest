package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

interface StreakRepository {
    fun getCurrentStreak(): Flow<StreakEntity?>
    suspend fun insertStreak(streak: StreakEntity)
    suspend fun updateStreak(streak: StreakEntity)
}
