package com.pixelquest.app.quran.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * An ayah the player bookmarked (from version 15), by its surah and number. It's the player's own,
 * not the Quran's: it doesn't point into the Quran's tables, so loading a newer bundle leaves it be.
 *
 * @param createdAt when it was bookmarked, in epoch millis.
 */
@Entity(tableName = "quran_bookmark", primaryKeys = ["surah", "ayah"])
data class QuranBookmarkEntity(
    val surah: Int,
    val ayah: Int,
    val createdAt: Long
)

/** The player's bookmarks. */
@Dao
interface QuranBookmarkDao {
    /** All of them, in the Quran's order. */
    @Query("SELECT * FROM quran_bookmark ORDER BY surah, ayah")
    fun observeAll(): Flow<List<QuranBookmarkEntity>>

    /** The bookmarked ayahs of [surah], by number. */
    @Query("SELECT ayah FROM quran_bookmark WHERE surah = :surah ORDER BY ayah")
    fun observeIn(surah: Int): Flow<List<Int>>

    /** Bookmarking an ayah twice keeps the first time. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: QuranBookmarkEntity)

    @Query("DELETE FROM quran_bookmark WHERE surah = :surah AND ayah = :ayah")
    suspend fun delete(surah: Int, ayah: Int)

    @Query("SELECT COUNT(*) FROM quran_bookmark")
    suspend fun count(): Int
}
