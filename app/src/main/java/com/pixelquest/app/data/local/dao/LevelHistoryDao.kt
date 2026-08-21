package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevelHistory(entry: LevelHistoryEntity)

    @Query("SELECT * FROM level_history ORDER BY achievedDate DESC")
    fun getAllHistory(): Flow<List<LevelHistoryEntity>>
}
