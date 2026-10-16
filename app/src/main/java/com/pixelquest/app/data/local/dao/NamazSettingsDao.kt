package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelquest.app.data.local.entity.NamazSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NamazSettingsDao {

    /** The settings row as it changes; null until the tracker's settings are first saved. */
    @Query("SELECT * FROM namaz_settings WHERE id = ${NamazSettingsEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<NamazSettingsEntity?>

    @Query("SELECT * FROM namaz_settings WHERE id = ${NamazSettingsEntity.SINGLE_ROW_ID}")
    suspend fun get(): NamazSettingsEntity?

    /** Writes the whole row (there's only ever the one). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: NamazSettingsEntity)
}
