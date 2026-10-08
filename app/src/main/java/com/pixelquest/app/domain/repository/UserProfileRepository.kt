package com.pixelquest.app.domain.repository

import com.pixelquest.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface UserProfileRepository {
    fun getProfile(): Flow<UserProfileEntity?>
    suspend fun insertProfile(profile: UserProfileEntity)
    suspend fun updateProfile(profile: UserProfileEntity)
    suspend fun performLevelUp(): UserProfileEntity?
    suspend fun updateSupabaseUserId(userId: String?)
    suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?)
    suspend fun updateLeaderboardOptIn(optIn: Boolean)
    suspend fun clearCloudData()

    /** Adds [points] to the total XP without rewriting the rest of the profile. */
    suspend fun addXp(points: Int) {
        getProfile().first()?.let { updateProfile(it.copy(totalXp = it.totalXp + points)) }
    }

    /** Sets the level and the perfect days toward the next one, leaving the rest of the profile alone. */
    suspend fun setLevelProgress(level: Int, perfectDaysTowardNextLevel: Int) {
        getProfile().first()?.let { updateProfile(it.copy(level = level, perfectDaysTowardNextLevel = perfectDaysTowardNextLevel)) }
    }
}
