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

    /** For the first-launch seed: adds the row only if it doesn't exist, so the player's own choices win. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfileIfAbsent(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    /**
     * Adds XP in one statement. Completions and the nightly level check used to read the whole
     * profile and write it back, so whichever wrote second put back the other's old values.
     */
    @Query("UPDATE user_profile SET totalXp = totalXp + :points WHERE id = 1")
    suspend fun addXp(points: Int)

    /** Level and progress only (see [addXp]). */
    @Query("UPDATE user_profile SET level = :level, perfectDaysTowardNextLevel = :progress WHERE id = 1")
    suspend fun setLevelProgress(level: Int, progress: Int)

    @Query("UPDATE user_profile SET supabaseUserId = :userId WHERE id = 1")
    suspend fun updateSupabaseUserId(userId: String?)

    @Query("UPDATE user_profile SET leaderboardOptIn = :optIn, leaderboardDisplayName = :displayName WHERE id = 1")
    suspend fun updateLeaderboardSettings(optIn: Boolean, displayName: String?)

    @Query("UPDATE user_profile SET leaderboardOptIn = :optIn WHERE id = 1")
    suspend fun updateLeaderboardOptIn(optIn: Boolean)
}
