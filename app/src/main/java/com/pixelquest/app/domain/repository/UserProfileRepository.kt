package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getProfile(): Flow<UserProfileEntity?>
    suspend fun insertProfile(profile: UserProfileEntity)
    suspend fun updateProfile(profile: UserProfileEntity)
}
