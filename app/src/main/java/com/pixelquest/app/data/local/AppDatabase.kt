package com.pixelquest.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pixelquest.app.data.local.dao.DifficultySettingsDao
import com.pixelquest.app.data.local.dao.InsightCacheDao
import com.pixelquest.app.data.local.dao.LevelHistoryDao
import com.pixelquest.app.data.local.dao.StreakDao
import com.pixelquest.app.data.local.dao.TaskCompletionLogDao
import com.pixelquest.app.data.local.dao.TaskDao
import com.pixelquest.app.data.local.dao.UserProfileDao
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        TaskEntity::class,
        StreakEntity::class,
        UserProfileEntity::class,
        DifficultySettingsEntity::class,
        TaskCompletionLogEntity::class,
        LevelHistoryEntity::class,
        InsightCacheEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun streakDao(): StreakDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun difficultySettingsDao(): DifficultySettingsDao
    abstract fun taskCompletionLogDao(): TaskCompletionLogDao
    abstract fun levelHistoryDao(): LevelHistoryDao
    abstract fun insightCacheDao(): InsightCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN perfectDaysTowardNextLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `level_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `level` INTEGER NOT NULL, `achievedDate` INTEGER NOT NULL, `difficultyAtTimeOfLevelUp` TEXT NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN supabaseUserId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN leaderboardOptIn INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN leaderboardDisplayName TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `insight_cache` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `suggestion` TEXT NOT NULL,
                        `encouragement` TEXT NOT NULL,
                        `highlightCategory` TEXT,
                        `specificTaskCallout` TEXT,
                        `dataHash` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Day 26: per-task reminder settings. Existing tasks keep today's behaviour. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderLeadMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderStyle TEXT NOT NULL DEFAULT 'STANDARD'")
            }
        }

        /**
         * Day 27: store the weekdays a weekly task repeats on (bitmask, Monday = bit 0). Existing
         * weekly tasks get their first date's weekday, so they keep repeating exactly as before.
         * strftime('%w') is 0 for Sunday, so (w + 6) % 7 turns it into Monday = 0 ... Sunday = 6.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN weeklyDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE tasks SET weeklyDays = 1 << ((CAST(strftime('%w', scheduledDay) AS INTEGER) + 6) % 7) " +
                        "WHERE recurrenceType = 'WEEKLY'"
                )
            }
        }
    }
}
