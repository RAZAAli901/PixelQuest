package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AllMigrationsTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Base version 1 tables
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `scheduledDay` TEXT NOT NULL, `scheduledTime` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `recurrenceType` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `category` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `streaks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `currentStreak` INTEGER NOT NULL, `bestStreak` INTEGER NOT NULL, `lastCompletedDate` TEXT, `startDate` TEXT, `isFrozen` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `avatarPath` TEXT NOT NULL, `totalXp` INTEGER NOT NULL, `level` INTEGER NOT NULL, `streakMultiplier` REAL NOT NULL, `joinDate` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `difficulty_settings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `difficulty` TEXT NOT NULL, `xpMultiplier` REAL NOT NULL, `hardcoreMode` INTEGER NOT NULL, `strictDeadline` INTEGER NOT NULL, `streakDecay` INTEGER NOT NULL)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `task_completion_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `completedDate` TEXT NOT NULL, `wasCompleted` INTEGER NOT NULL, `pointsAwarded` INTEGER NOT NULL)",
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    private fun columnExists(table: String, column: String): Boolean {
        val cursor = db.query("PRAGMA table_info(`$table`)")
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (it.getString(nameIndex) == column) return true
            }
        }
        return false
    }

    private fun tableExists(table: String): Boolean {
        val cursor = db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='$table'")
        cursor.use {
            return it.moveToFirst()
        }
    }

    @Test
    fun allMigrations_hasContinuousChainFromVersion1To23() {
        val migrations = AppDatabase.ALL_MIGRATIONS
        assertEquals(22, migrations.size)
        var currentVersion = 1
        for (m in migrations) {
            assertEquals("Start version matches current chain", currentVersion, m.startVersion)
            assertEquals("End version advances by 1", currentVersion + 1, m.endVersion)
            currentVersion = m.endVersion
        }
        assertEquals(23, currentVersion)
    }

    @Test
    fun migration_1_to_2_creates_level_history_and_adds_column() {
        AppDatabase.MIGRATION_1_2.migrate(db)
        assertTrue(columnExists("user_profile", "perfectDaysTowardNextLevel"))
        assertTrue(tableExists("level_history"))
    }

    @Test
    fun migration_2_to_3_adds_leaderboard_columns() {
        AppDatabase.MIGRATION_2_3.migrate(db)
        assertTrue(columnExists("user_profile", "supabaseUserId"))
        assertTrue(columnExists("user_profile", "leaderboardOptIn"))
        assertTrue(columnExists("user_profile", "leaderboardDisplayName"))
    }

    @Test
    fun migration_3_to_4_creates_insight_cache() {
        AppDatabase.MIGRATION_3_4.migrate(db)
        assertTrue(tableExists("insight_cache"))
    }

    @Test
    fun migration_4_to_5_adds_reminder_columns() {
        AppDatabase.MIGRATION_4_5.migrate(db)
        assertTrue(columnExists("tasks", "reminderEnabled"))
        assertTrue(columnExists("tasks", "reminderLeadMinutes"))
        assertTrue(columnExists("tasks", "reminderStyle"))
    }

    @Test
    fun migration_5_to_6_adds_weekly_days() {
        AppDatabase.MIGRATION_5_6.migrate(db)
        assertTrue(columnExists("tasks", "weeklyDays"))
    }

    @Test
    fun migration_6_to_7_deduplicates_logs_and_creates_unique_index() {
        AppDatabase.MIGRATION_6_7.migrate(db)
        val cursor = db.query("PRAGMA index_list(`task_completion_logs`)")
        var hasUniqueIndex = false
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            val uniqueIndex = it.getColumnIndex("unique")
            while (it.moveToNext()) {
                if (it.getInt(uniqueIndex) == 1 && it.getString(nameIndex).contains("taskId_completedDate")) {
                    hasUniqueIndex = true
                }
            }
        }
        assertTrue(hasUniqueIndex)
    }

    @Test
    fun migration_7_to_8_creates_namaz_tables() {
        AppDatabase.MIGRATION_7_8.migrate(db)
        assertTrue(tableExists("namaz_log"))
        assertTrue(tableExists("namaz_settings"))
    }

    @Test
    fun migration_8_to_9_adds_bonus_xp() {
        AppDatabase.MIGRATION_7_8.migrate(db)
        AppDatabase.MIGRATION_8_9.migrate(db)
        assertTrue(columnExists("namaz_log", "bonusXp"))
    }

    @Test
    fun migration_9_to_10_creates_darood_tables() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        assertTrue(tableExists("darood_party"))
        assertTrue(tableExists("darood_party_member"))
        assertTrue(tableExists("darood_entry"))
        assertTrue(tableExists("darood_pending_entry"))
    }

    @Test
    fun migration_10_to_11_adds_status_changed_at() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_10_11.migrate(db)
        assertTrue(columnExists("darood_party_member", "statusChangedAt"))
    }

    @Test
    fun migration_11_to_12_adds_is_public_race() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_11_12.migrate(db)
        assertTrue(columnExists("darood_party", "isPublicRace"))
    }

    @Test
    fun migration_12_to_13_creates_quran_tables() {
        AppDatabase.MIGRATION_12_13.migrate(db)
        assertTrue(tableExists("quran_surah"))
        assertTrue(tableExists("quran_ayah"))
        assertTrue(tableExists("quran_translation"))
    }

    @Test
    fun migration_13_to_14_creates_quran_juz() {
        AppDatabase.MIGRATION_13_14.migrate(db)
        assertTrue(tableExists("quran_juz"))
    }

    @Test
    fun migration_14_to_15_creates_quran_bookmark() {
        AppDatabase.MIGRATION_14_15.migrate(db)
        assertTrue(tableExists("quran_bookmark"))
    }

    @Test
    fun migration_15_to_16_adds_arabic_plain() {
        AppDatabase.MIGRATION_12_13.migrate(db)
        AppDatabase.MIGRATION_15_16.migrate(db)
        assertTrue(columnExists("quran_ayah", "arabicPlain"))
    }

    @Test
    fun migration_16_to_17_adds_party_type() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_16_17.migrate(db)
        assertTrue(columnExists("darood_party", "partyType"))
    }

    @Test
    fun migration_17_to_18_creates_study_tables() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_17_18.migrate(db)
        assertTrue(tableExists("study_item"))
        assertTrue(tableExists("study_completion"))
        assertTrue(tableExists("study_session"))
        assertTrue(tableExists("study_week_score"))
        assertTrue(tableExists("study_week_winner"))
        assertTrue(tableExists("study_pending"))
    }

    @Test
    fun migration_18_to_19_creates_study_reminder_tables() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_18_19.migrate(db)
        assertTrue(tableExists("study_reminder"))
        assertTrue(tableExists("study_reminder_pending"))
        assertTrue(tableExists("study_reminder_done"))
    }

    @Test
    fun migration_19_to_20_creates_chat_tables() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_19_20.migrate(db)
        assertTrue(tableExists("chat_message"))
        assertTrue(tableExists("chat_message_pending"))
        assertTrue(tableExists("chat_file"))
        assertTrue(tableExists("chat_upload"))
        assertTrue(tableExists("chat_read"))
    }

    @Test
    fun migration_20_to_21_creates_week_result_tables() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_20_21.migrate(db)
        assertTrue(tableExists("darood_week_result"))
        assertTrue(tableExists("darood_week_notified"))
    }

    @Test
    fun migration_21_to_22_creates_chess_tables() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_21_22.migrate(db)
        assertTrue(tableExists("chess_rating"))
        assertTrue(tableExists("chess_head_to_head"))
        assertTrue(tableExists("chess_finished_game"))
    }

    @Test
    fun migration_22_to_23_adds_chat_message_ref_id() {
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_19_20.migrate(db)
        AppDatabase.MIGRATION_22_23.migrate(db)
        assertTrue(columnExists("chat_message", "refId"))
    }

    @Test
    fun full_end_to_end_migration_from_1_to_23_runs_sequentially_without_error() {
        for (migration in AppDatabase.ALL_MIGRATIONS) {
            migration.migrate(db)
        }
        // Verify final schema has representative tables and columns from all domains
        assertTrue(tableExists("level_history"))
        assertTrue(tableExists("insight_cache"))
        assertTrue(tableExists("namaz_log"))
        assertTrue(tableExists("darood_party"))
        assertTrue(tableExists("quran_surah"))
        assertTrue(tableExists("quran_juz"))
        assertTrue(tableExists("quran_bookmark"))
        assertTrue(tableExists("study_item"))
        assertTrue(tableExists("study_reminder"))
        assertTrue(tableExists("chat_message"))
        assertTrue(tableExists("darood_week_result"))
        assertTrue(tableExists("chess_rating"))
        assertTrue(columnExists("chat_message", "refId"))
    }
}
