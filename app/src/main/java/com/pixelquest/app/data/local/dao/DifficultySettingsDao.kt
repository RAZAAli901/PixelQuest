package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DifficultySettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: DifficultySettingsEntity)

    @Update
    suspend fun updateSettings(settings: DifficultySettingsEntity)

    @Query("SELECT * FROM difficulty_settings WHERE id = 1")
    fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?>
}
