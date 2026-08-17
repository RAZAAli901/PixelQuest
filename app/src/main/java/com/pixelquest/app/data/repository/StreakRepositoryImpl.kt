package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.StreakDao
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.domain.repository.StreakRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StreakRepositoryImpl @Inject constructor(
    private val streakDao: StreakDao
) : StreakRepository {
    override fun getCurrentStreak(): Flow<StreakEntity?> = streakDao.getCurrentStreak()
    override suspend fun insertStreak(streak: StreakEntity) = streakDao.insertStreak(streak)
    override suspend fun updateStreak(streak: StreakEntity) = streakDao.updateStreak(streak)
}
