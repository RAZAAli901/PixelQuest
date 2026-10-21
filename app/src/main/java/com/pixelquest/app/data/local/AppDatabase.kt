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
        com.pixelquest.app.data.local.entity.NamazSettingsEntity::class,
        com.pixelquest.app.party.data.local.PartyEntity::class,
        com.pixelquest.app.party.data.local.PartyMemberEntity::class,
        com.pixelquest.app.darood.data.local.DaroodEntryEntity::class,
        com.pixelquest.app.darood.data.local.PendingEntryEntity::class,
        // The Quran (docs/quran-data.md), from version 13: loaded from the app's own files.
        com.pixelquest.app.quran.data.local.QuranSurahEntity::class,
        com.pixelquest.app.quran.data.local.QuranAyahEntity::class,
        com.pixelquest.app.quran.data.local.QuranAyahFtsEntity::class,
        com.pixelquest.app.quran.data.local.TranslationInfoEntity::class,
        // Where each juz starts, from version 14.
        com.pixelquest.app.quran.data.local.QuranJuzEntity::class,
        // The player's Quran bookmarks, from version 15; the ayahs' Arabic search key, from 16.
        com.pixelquest.app.quran.data.local.QuranBookmarkEntity::class,
        // Study parties' cache (docs/study-party.md), from version 18.
        com.pixelquest.app.study.data.local.StudyItemEntity::class,
        com.pixelquest.app.study.data.local.StudyCompletionEntity::class,
        com.pixelquest.app.study.data.local.StudySessionEntity::class,
        com.pixelquest.app.study.data.local.StudyWeekScoreEntity::class,
        com.pixelquest.app.study.data.local.StudyWeekWinnerEntity::class,
        com.pixelquest.app.study.data.local.PendingStudyEntity::class,
        // Study reminders (docs/study-reminders.md), from version 19.
        com.pixelquest.app.study.data.local.StudyReminderEntity::class,
        com.pixelquest.app.study.data.local.PendingStudyReminderEntity::class,
        com.pixelquest.app.study.data.local.StudyReminderDoneEntity::class,
        // Study chat (docs/study-chat.md), from version 20.
        com.pixelquest.app.study.chat.data.local.ChatMessageEntity::class,
        com.pixelquest.app.study.chat.data.local.PendingChatMessageEntity::class,
        com.pixelquest.app.study.chat.data.local.ChatFileEntity::class,
        com.pixelquest.app.study.chat.data.local.ChatUploadEntity::class,
        com.pixelquest.app.study.chat.data.local.ChatReadEntity::class,
        // Darood Parties' closed weeks (docs/darood-weekly.md), from version 21.
        com.pixelquest.app.darood.data.local.DaroodWeekResultEntity::class,
        com.pixelquest.app.darood.data.local.DaroodWeekNotifiedEntity::class,
        // Chess's cache (docs/chess-design.md), from version 22: never a live game.
        com.pixelquest.app.chess.data.local.ChessRatingEntity::class,
        com.pixelquest.app.chess.data.local.ChessHeadToHeadEntity::class,
        com.pixelquest.app.chess.data.local.ChessFinishedGameEntity::class
    ],
    version = 23,
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
    abstract fun daroodDao(): com.pixelquest.app.darood.data.local.DaroodDao
    abstract fun partyDao(): com.pixelquest.app.party.data.local.PartyDao
    abstract fun quranDao(): com.pixelquest.app.quran.data.local.QuranDao
    abstract fun quranBookmarkDao(): com.pixelquest.app.quran.data.local.QuranBookmarkDao
    abstract fun studyDao(): com.pixelquest.app.study.data.local.StudyDao
    abstract fun studyReminderDao(): com.pixelquest.app.study.data.local.StudyReminderDao
    abstract fun chatDao(): com.pixelquest.app.study.chat.data.local.ChatDao
    abstract fun chessDao(): com.pixelquest.app.chess.data.local.ChessDao

    companion object {
        /**
         * Every migration, in order: the app and DatabaseUpgradeTest use this one list, which the test
         * checks reaches the current version from each earlier one (layouts in app/schemas).
         */
        val ALL_MIGRATIONS: Array<Migration> by lazy {
            arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23)
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

        /**
         * Darood Party's cache (docs/darood-design.md): parties, their members and entries, and the
         * entries waiting to be sent. Four new tables, which start empty and fill from the server;
         * nothing existing changes. The SQL is exactly what Room exported for version 10
         * (app/schemas/.../10.json).
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `darood_party` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `leaderId` TEXT, `maxMembers` INTEGER NOT NULL, `milestone` INTEGER, `milestoneSetAt` INTEGER, `completedAt` INTEGER, `createdAt` INTEGER NOT NULL, `total` INTEGER NOT NULL, `entryCount` INTEGER NOT NULL, `memberCount` INTEGER NOT NULL, `lastEntryDate` TEXT, `myRole` TEXT NOT NULL, `myStatus` TEXT NOT NULL, `syncedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `darood_party_member` (`partyId` TEXT NOT NULL, `userId` TEXT NOT NULL, `username` TEXT, `role` TEXT NOT NULL, `status` TEXT NOT NULL, `invitedAt` INTEGER, `joinedAt` INTEGER, `total` INTEGER NOT NULL DEFAULT 0, `activeDays` INTEGER NOT NULL DEFAULT 0, `lastEntryDate` TEXT, PRIMARY KEY(`partyId`, `userId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_darood_party_member_partyId` ON `darood_party_member` (`partyId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `darood_entry` (`id` INTEGER NOT NULL, `partyId` TEXT NOT NULL, `userId` TEXT, `amount` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `tz` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `clientId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_darood_entry_partyId_id` ON `darood_entry` (`partyId`, `id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_darood_entry_clientId` ON `darood_entry` (`clientId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `darood_pending_entry` (`clientId` TEXT NOT NULL, `partyId` TEXT NOT NULL, `amount` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `tz` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `state` TEXT NOT NULL, `attempts` INTEGER NOT NULL DEFAULT 0, `refusal` TEXT, PRIMARY KEY(`clientId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_darood_pending_entry_partyId` ON `darood_pending_entry` (`partyId`)")
            }
        }

        /**
         * Version 11: when each Darood Party member's row last changed on the server, so the chat can
         * say when someone left or was removed. Empty until the next refresh brings it.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `darood_party_member` ADD COLUMN `statusChangedAt` INTEGER")
            }
        }

        /** Version 12: whether a Darood Party is on the race board. Off until the next refresh says. */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `darood_party` ADD COLUMN `isPublicRace` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** The Quran's tables (docs/quran-data.md), empty until its loader fills them from the app's files. */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                QURAN_TABLES_13.forEach { db.execSQL(it) }
            }
        }

        /** Where each juz starts: empty until the Quran's loader fills it (it loads again to do so). */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `quran_juz` (`number` INTEGER NOT NULL, `surah` INTEGER NOT NULL, `ayah` INTEGER NOT NULL, PRIMARY KEY(`number`))")
            }
        }

        /** The player's Quran bookmarks: empty until they bookmark an ayah. */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `quran_bookmark` (`surah` INTEGER NOT NULL, `ayah` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`surah`, `ayah`))")
            }
        }

        /**
         * Each ayah's Arabic as search matches it: empty until the Quran's loader fills it (finding
         * it empty, it loads again to do so).
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `quran_ayah` ADD COLUMN `arabicPlain` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Each cached party's kind (docs/party-core.md): every party cached before is a Darood Party.
         * The cache's tables keep their names (darood_party, darood_party_member), so nothing moves.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `darood_party` ADD COLUMN `partyType` TEXT NOT NULL DEFAULT 'DAROOD'")
            }
        }

        /**
         * Study parties' cache (docs/study-party.md): items, completions, study time, scores, winners,
         * and what's waiting to be sent. Six new tables, which start empty and fill from the server;
         * nothing existing changes. The SQL is exactly what Room exported for version 18
         * (app/schemas/.../18.json).
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                STUDY_TABLES_18.forEach { db.execSQL(it) }
            }
        }

        /**
         * Study reminders (docs/study-reminders.md): the server's, those waiting to be sent, and
         * which alerts are done on this phone. Three new tables, empty until the first sync; nothing
         * existing changes. The SQL is exactly what Room exported for version 19 (19.json).
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                REMINDER_TABLES_19.forEach { db.execSQL(it) }
            }
        }

        /**
         * The study chat (docs/study-chat.md): messages, those waiting to be sent, files, uploads
         * under way, and how far each chat is read. Five new tables, empty until the first refresh;
         * nothing existing changes. The SQL is exactly what Room exported for version 20 (20.json).
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                CHAT_TABLES_20.forEach { db.execSQL(it) }
            }
        }

        /**
         * Darood Parties' closed weeks (docs/darood-weekly.md): each member's place, and which weeks
         * this phone has told the player about. Two new tables, empty until the first refresh;
         * nothing existing changes. The SQL is exactly what Room exported for version 21 (21.json).
         */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                WEEK_TABLES_21.forEach { db.execSQL(it) }
            }
        }

        /**
         * Chess's cache (docs/chess-design.md): ratings, head-to-head records and finished games.
         * Three new tables, empty until the first refresh; nothing existing changes. The SQL is
         * exactly what Room exported for version 22 (22.json).
         */
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                CHESS_TABLES_22.forEach { db.execSQL(it) }
            }
        }

        /**
         * The study chat's server lines say what they're about (refId: a chess challenge or game,
         * docs/chess-ui-integration.md). One nullable column; every message kept has none.
         */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `refId` TEXT")
            }
        }

        /** Version 22's chess tables, as Room exports them (app/schemas, 22.json). */
        private val CHESS_TABLES_22 = listOf(
            "CREATE TABLE IF NOT EXISTS `chess_rating` (`userId` TEXT NOT NULL, `timeControl` INTEGER NOT NULL, `points` INTEGER NOT NULL, `games` INTEGER NOT NULL, `wins` INTEGER NOT NULL, `draws` INTEGER NOT NULL, `losses` INTEGER NOT NULL, `peak` INTEGER NOT NULL, PRIMARY KEY(`userId`, `timeControl`))",
            "CREATE TABLE IF NOT EXISTS `chess_head_to_head` (`me` TEXT NOT NULL, `them` TEXT NOT NULL, `timeControl` INTEGER NOT NULL, `myWins` INTEGER NOT NULL, `theirWins` INTEGER NOT NULL, `draws` INTEGER NOT NULL, PRIMARY KEY(`me`, `them`, `timeControl`))",
            "CREATE TABLE IF NOT EXISTS `chess_finished_game` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `whiteId` TEXT NOT NULL, `blackId` TEXT NOT NULL, `timeControl` INTEGER NOT NULL, `rated` INTEGER NOT NULL, `result` TEXT, `reason` TEXT, `fen` TEXT NOT NULL, `ply` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `whiteChange` INTEGER, `blackChange` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_chess_finished_game_partyId_endedAt` ON `chess_finished_game` (`partyId`, `endedAt`)"
        )

        /** Version 21's week tables, as Room exports them (app/schemas, 21.json). */
        private val WEEK_TABLES_21 = listOf(
            "CREATE TABLE IF NOT EXISTS `darood_week_result` (`partyId` TEXT NOT NULL, `weekStart` TEXT NOT NULL, `userId` TEXT NOT NULL, `position` INTEGER NOT NULL, `weekTotal` INTEGER NOT NULL, `activeDays` INTEGER NOT NULL, `closedAt` INTEGER NOT NULL, PRIMARY KEY(`partyId`, `weekStart`, `userId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `darood_week_notified` (`partyId` TEXT NOT NULL, `weekStart` TEXT NOT NULL, `notifiedAt` INTEGER NOT NULL, PRIMARY KEY(`partyId`, `weekStart`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        /** Version 20's chat tables, as Room exports them (app/schemas, 20.json). */
        private val CHAT_TABLES_20 = listOf(
            "CREATE TABLE IF NOT EXISTS `chat_message` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `senderId` TEXT, `kind` TEXT NOT NULL, `body` TEXT, `fileId` TEXT, `replyTo` TEXT, `clientId` TEXT, `createdAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_chat_message_partyId_createdAt` ON `chat_message` (`partyId`, `createdAt`)",
            "CREATE INDEX IF NOT EXISTS `index_chat_message_clientId` ON `chat_message` (`clientId`)",
            "CREATE TABLE IF NOT EXISTS `chat_message_pending` (`clientId` TEXT NOT NULL, `partyId` TEXT NOT NULL, `body` TEXT NOT NULL, `replyTo` TEXT, `createdAt` INTEGER NOT NULL, `state` TEXT NOT NULL, `attempts` INTEGER NOT NULL DEFAULT 0, `refusal` TEXT, PRIMARY KEY(`clientId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_chat_message_pending_partyId` ON `chat_message_pending` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `chat_file` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `uploaderId` TEXT, `displayName` TEXT NOT NULL, `mime` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `sha256` TEXT, `status` TEXT NOT NULL, `rejectReason` TEXT, `pinned` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `readyAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_chat_file_partyId_createdAt` ON `chat_file` (`partyId`, `createdAt`)",
            "CREATE TABLE IF NOT EXISTS `chat_upload` (`clientId` TEXT NOT NULL, `partyId` TEXT NOT NULL, `uri` TEXT NOT NULL, `displayName` TEXT NOT NULL, `mime` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `fileId` TEXT, `state` TEXT NOT NULL, `sentBytes` INTEGER NOT NULL DEFAULT 0, `reason` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`clientId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_chat_upload_partyId` ON `chat_upload` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `chat_read` (`partyId` TEXT NOT NULL, `lastReadAt` INTEGER NOT NULL, PRIMARY KEY(`partyId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        /** Version 19's reminder tables, as Room exports them (app/schemas, 19.json). */
        private val REMINDER_TABLES_19 = listOf(
            "CREATE TABLE IF NOT EXISTS `study_reminder` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `createdBy` TEXT, `title` TEXT NOT NULL, `kind` TEXT NOT NULL, `scope` TEXT NOT NULL, `dueAt` INTEGER, `weekStart` TEXT, `tz` TEXT NOT NULL, `repeat` TEXT NOT NULL, `audience` TEXT NOT NULL, `alertOffsets` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_study_reminder_partyId` ON `study_reminder` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `study_reminder_pending` (`clientId` TEXT NOT NULL, `op` TEXT NOT NULL, `partyId` TEXT NOT NULL, `reminderId` TEXT, `title` TEXT, `kind` TEXT, `scope` TEXT, `dueAt` INTEGER, `weekStart` TEXT, `tz` TEXT, `repeat` TEXT, `audience` TEXT, `alertOffsets` TEXT, `createdAt` INTEGER NOT NULL, `state` TEXT NOT NULL, `attempts` INTEGER NOT NULL DEFAULT 0, `refusal` TEXT, PRIMARY KEY(`clientId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_study_reminder_pending_partyId` ON `study_reminder_pending` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `study_reminder_done` (`reminderId` TEXT NOT NULL, `occurrenceAt` INTEGER NOT NULL, `doneAt` INTEGER NOT NULL, PRIMARY KEY(`reminderId`, `occurrenceAt`))"
        )

        /** Version 18's study tables, as Room exports them (app/schemas, 18.json). */
        private val STUDY_TABLES_18 = listOf(
            "CREATE TABLE IF NOT EXISTS `study_item` (`id` TEXT NOT NULL, `partyId` TEXT NOT NULL, `kind` TEXT NOT NULL, `title` TEXT NOT NULL, `subject` TEXT, `notes` TEXT, `dueAt` INTEGER, `pinned` INTEGER NOT NULL, `createdBy` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_study_item_partyId` ON `study_item` (`partyId`)",
            "CREATE TABLE IF NOT EXISTS `study_completion` (`id` INTEGER NOT NULL, `partyId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `userId` TEXT, `completedAt` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `tz` TEXT NOT NULL, `clientId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_study_completion_partyId` ON `study_completion` (`partyId`)",
            "CREATE INDEX IF NOT EXISTS `index_study_completion_itemId` ON `study_completion` (`itemId`)",
            "CREATE INDEX IF NOT EXISTS `index_study_completion_clientId` ON `study_completion` (`clientId`)",
            "CREATE TABLE IF NOT EXISTS `study_session` (`id` INTEGER NOT NULL, `partyId` TEXT NOT NULL, `userId` TEXT, `minutes` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `tz` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `clientId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_study_session_partyId` ON `study_session` (`partyId`)",
            "CREATE INDEX IF NOT EXISTS `index_study_session_clientId` ON `study_session` (`clientId`)",
            "CREATE TABLE IF NOT EXISTS `study_week_score` (`partyId` TEXT NOT NULL, `userId` TEXT NOT NULL, `weekStart` TEXT NOT NULL, `points` INTEGER NOT NULL, `itemPoints` INTEGER NOT NULL, `bonusPoints` INTEGER NOT NULL, `studyPoints` INTEGER NOT NULL, `itemsCompleted` INTEGER NOT NULL, `studyMinutes` INTEGER NOT NULL, PRIMARY KEY(`partyId`, `userId`, `weekStart`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `study_week_winner` (`partyId` TEXT NOT NULL, `weekStart` TEXT NOT NULL, `userId` TEXT NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`partyId`, `weekStart`, `userId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `study_pending` (`clientId` TEXT NOT NULL, `kind` TEXT NOT NULL, `partyId` TEXT NOT NULL, `itemId` TEXT, `minutes` INTEGER, `localDate` TEXT NOT NULL, `tz` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `state` TEXT NOT NULL, `attempts` INTEGER NOT NULL DEFAULT 0, `refusal` TEXT, PRIMARY KEY(`clientId`), FOREIGN KEY(`partyId`) REFERENCES `darood_party`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_study_pending_partyId` ON `study_pending` (`partyId`)"
        )

        /** Version 13's Quran tables, as Room exports them (app/schemas, 13.json): its search index's triggers too. */
        private val QURAN_TABLES_13 = listOf(
            "CREATE TABLE IF NOT EXISTS `quran_surah` (`number` INTEGER NOT NULL, `arabicName` TEXT NOT NULL, `englishName` TEXT NOT NULL, `englishMeaning` TEXT NOT NULL, `revelation` TEXT NOT NULL, `revelationOrder` INTEGER NOT NULL, `ayahCount` INTEGER NOT NULL, `juz` INTEGER NOT NULL, `bismillah` TEXT, PRIMARY KEY(`number`))",
            "CREATE TABLE IF NOT EXISTS `quran_ayah` (`id` INTEGER NOT NULL, `surah` INTEGER NOT NULL, `ayah` INTEGER NOT NULL, `arabic` TEXT NOT NULL, `translationId` TEXT NOT NULL, `translation` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_quran_ayah_surah_ayah` ON `quran_ayah` (`surah`, `ayah`)",
            "CREATE VIRTUAL TABLE IF NOT EXISTS `quran_ayah_fts` USING FTS4(`translation` TEXT NOT NULL, tokenize=unicode61, content=`quran_ayah`)",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_quran_ayah_fts_BEFORE_UPDATE BEFORE UPDATE ON `quran_ayah` BEGIN DELETE FROM `quran_ayah_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_quran_ayah_fts_BEFORE_DELETE BEFORE DELETE ON `quran_ayah` BEGIN DELETE FROM `quran_ayah_fts` WHERE `docid`=OLD.`rowid`; END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_quran_ayah_fts_AFTER_UPDATE AFTER UPDATE ON `quran_ayah` BEGIN INSERT INTO `quran_ayah_fts`(`docid`, `translation`) VALUES (NEW.`rowid`, NEW.`translation`); END",
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_quran_ayah_fts_AFTER_INSERT AFTER INSERT ON `quran_ayah` BEGIN INSERT INTO `quran_ayah_fts`(`docid`, `translation`) VALUES (NEW.`rowid`, NEW.`translation`); END",
            "CREATE TABLE IF NOT EXISTS `quran_translation` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `translator` TEXT NOT NULL, `language` TEXT NOT NULL, `lastUpdate` TEXT NOT NULL, `licenceNote` TEXT NOT NULL, `notice` TEXT NOT NULL, `dataVersion` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}
