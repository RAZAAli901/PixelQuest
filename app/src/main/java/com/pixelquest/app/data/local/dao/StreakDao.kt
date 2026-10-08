package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelquest.app.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity)

    /** For the first-launch seed: adds the row only if it doesn't exist, so the player's own choices win. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStreakIfAbsent(streak: StreakEntity)

    @Update
    suspend fun updateStreak(streak: StreakEntity)

    @Query("SELECT * FROM streaks WHERE id = 1")
    fun getCurrentStreak(): Flow<StreakEntity?>
}
