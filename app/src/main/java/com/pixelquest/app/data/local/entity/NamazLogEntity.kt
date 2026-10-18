package com.pixelquest.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerStatus
import java.time.LocalDate

/**
 * One prayer on one prayer day (docs/namaz-design.md). The unique index keeps one row per prayer
 * per day, so marking it twice (a double tap, a notification action) updates rather than adds.
 *
 * @param date the prayer day: before that day's Fajr, a moment belongs to the day before.
 * @param prayedAt epoch millis when it was marked prayed; null otherwise.
 * @param xpAwarded what marking it earned for the prayer itself, so an undo takes back exactly that.
 * @param bonusXp the bonuses marking it unlocked (all five on the day, a week's run): taken back with
 *   it, and taken back too if another prayer of the day is undone, since the day's no longer five
 *   (database version 9).
 */
@Entity(
    tableName = "namaz_log",
    indices = [Index(value = ["date", "prayer"], unique = true)]
)
data class NamazLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val prayer: Prayer,
    val status: PrayerStatus,
    val prayedAt: Long? = null,
    val xpAwarded: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val bonusXp: Int = 0
)
