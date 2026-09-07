package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelquest.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("UPDATE user_profile SET supabaseUserId = :userId WHERE id = 1")
    suspend fun updateSupabaseUserId(userId: String?)

    @Query("UPDATE user_profile SET leaderboardOptIn = :optIn, leaderboardDisplayName = :displayName WHERE id = 1")
    suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?)

    @Query("UPDATE user_profile SET leaderboardOptIn = :optIn WHERE id = 1")
    suspend fun updateLeaderboardOptIn(optIn: Boolean)
}
