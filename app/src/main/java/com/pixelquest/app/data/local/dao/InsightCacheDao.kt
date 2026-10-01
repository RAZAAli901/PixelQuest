package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Step 2: Room Data Access Object for local AI habit insight caching.
 */
@Dao
interface InsightCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: InsightCacheEntity): Long

    @Query("SELECT * FROM insight_cache ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestInsight(): InsightCacheEntity?

    @Query("SELECT * FROM insight_cache ORDER BY generatedAt DESC LIMIT 1")
    fun observeLatestInsight(): Flow<InsightCacheEntity?>

    @Query("DELETE FROM insight_cache WHERE generatedAt < :expiryTimestamp")
    suspend fun deleteOld(expiryTimestamp: Long): Int

    @Query("DELETE FROM insight_cache")
    suspend fun clearCache(): Int
}
