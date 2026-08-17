package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.dao.UserProfileDao
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {
    override fun getProfile(): Flow<UserProfileEntity?> = userProfileDao.getProfile()
    override suspend fun insertProfile(profile: UserProfileEntity) = userProfileDao.insertProfile(profile)
    override suspend fun updateProfile(profile: UserProfileEntity) = userProfileDao.updateProfile(profile)
}
