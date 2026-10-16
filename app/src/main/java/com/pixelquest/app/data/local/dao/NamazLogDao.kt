package com.pixelquest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelquest.app.data.local.entity.NamazLogEntity
import com.pixelquest.app.domain.namaz.Prayer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** How many prayers were marked prayed on one prayer day. */
data class NamazDayCount(val date: LocalDate, val prayed: Int)

@Dao
interface NamazLogDao {

    /**
     * Saves a prayer's row, replacing the one for the same day and prayer (the unique index): Room's
     * own upsert matches on the id, which a new row doesn't have yet.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NamazLogEntity)

    /** Adds the row unless that day already has one for the prayer (seeding a new day). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(log: NamazLogEntity): Long

    @Query("SELECT * FROM namaz_log WHERE date = :date AND prayer = :prayer")
    suspend fun get(date: LocalDate, prayer: Prayer): NamazLogEntity?

    @Query("SELECT * FROM namaz_log WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<NamazLogEntity>

    @Query("SELECT * FROM namaz_log WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<NamazLogEntity>>

    /** [from] to [to], inclusive (dates are stored as ISO text, which sorts by date). */
    @Query("SELECT * FROM namaz_log WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<NamazLogEntity>>

    @Query(
        "SELECT date AS date, SUM(CASE WHEN status = 'PRAYED' THEN 1 ELSE 0 END) AS prayed " +
            "FROM namaz_log WHERE date BETWEEN :from AND :to GROUP BY date ORDER BY date"
    )
    fun observePrayedPerDay(from: LocalDate, to: LocalDate): Flow<List<NamazDayCount>>

    /** Every row, for backups. */
    @Query("SELECT * FROM namaz_log ORDER BY date")
    suspend fun getAll(): List<NamazLogEntity>

    @Query("DELETE FROM namaz_log")
    suspend fun deleteAll()
}
