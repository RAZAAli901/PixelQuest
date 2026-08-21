package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.UserProfileDao
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.pixelquest.app.domain.LevelCalculator
import kotlinx.coroutines.flow.first

class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {
    override fun getProfile(): Flow<UserProfileEntity?> = userProfileDao.getProfile()
    override suspend fun insertProfile(profile: UserProfileEntity) = userProfileDao.insertProfile(profile)
    override suspend fun updateProfile(profile: UserProfileEntity) = userProfileDao.updateProfile(profile)

    override suspend fun performLevelUp(): UserProfileEntity? {
        val currentProfile = userProfileDao.getProfile().first() ?: return null
        val updated = currentProfile.copy(
            level = currentProfile.level + 1,
            perfectDaysTowardNextLevel = LevelCalculator.getPostLevelUpProgress()
        )
        userProfileDao.updateProfile(updated)
        return updated
    }
}
