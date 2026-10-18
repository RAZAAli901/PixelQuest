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
        InsightCacheEntity::class,
        // The Namaz Tracker (docs/namaz-design.md), from version 8.
        com.pixelquest.app.data.local.entity.NamazLogEntity::class,
        com.pixelquest.app.data.local.entity.NamazSettingsEntity::class
    ],
    version = 9,
    exportSchema = true
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
    abstract fun namazLogDao(): com.pixelquest.app.data.local.dao.NamazLogDao
    abstract fun namazSettingsDao(): com.pixelquest.app.data.local.dao.NamazSettingsDao

    companion object {
        /**
         * Every migration, in order: the app and DatabaseUpgradeTest use this one list, which the test
         * checks reaches the current version from each earlier one (layouts in app/schemas).
         */
        val ALL_MIGRATIONS: Array<Migration> by lazy {
            arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        }

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

        /**
         * Day 28: one completion log per task per day. Duplicates (a double tap, or the reminder's
         * "Yes" after completing in the app) are removed first, keeping a completed log over a missed
         * one and the newest among equals, then a unique index stops new ones.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM task_completion_logs WHERE id NOT IN (
                        SELECT COALESCE(
                            (SELECT MAX(done.id) FROM task_completion_logs done
                                WHERE done.taskId = l.taskId AND done.completedDate = l.completedDate
                                AND done.wasCompleted = 1),
                            MAX(l.id)
                        )
                        FROM task_completion_logs l
                        GROUP BY l.taskId, l.completedDate
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_completion_logs_taskId_completedDate` " +
                        "ON `task_completion_logs` (`taskId`, `completedDate`)"
                )
            }
        }

        /**
         * The Namaz Tracker's two tables (docs/namaz-design.md): the log, one row per prayer per day
         * with a unique (date, prayer) index, and the single settings row. Nothing existing changes.
         * The SQL is exactly what Room exported for version 8 (app/schemas/.../8.json).
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `namaz_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `prayer` TEXT NOT NULL, `status` TEXT NOT NULL, `prayedAt` INTEGER, `xpAwarded` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_namaz_log_date_prayer` ON `namaz_log` (`date`, `prayer`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `namaz_settings` (`id` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `method` TEXT NOT NULL, `asrMadhab` TEXT NOT NULL, `highLatitudeRule` TEXT, `latitude` REAL, `longitude` REAL, `city` TEXT, `reminderOffsetMin` INTEGER NOT NULL, `remindFajr` INTEGER NOT NULL, `remindDhuhr` INTEGER NOT NULL, `remindAsr` INTEGER NOT NULL, `remindMaghrib` INTEGER NOT NULL, `remindIsha` INTEGER NOT NULL, `adjustFajr` INTEGER NOT NULL, `adjustDhuhr` INTEGER NOT NULL, `adjustAsr` INTEGER NOT NULL, `adjustMaghrib` INTEGER NOT NULL, `adjustIsha` INTEGER NOT NULL, `xpEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        /** Namaz XP: the bonuses a prayer unlocked, apart from its own XP, so an undo can take back both exactly. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `namaz_log` ADD COLUMN `bonusXp` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
