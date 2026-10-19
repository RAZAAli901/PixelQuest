package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.model.ReminderStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Updating the app keeps the player's progress, from every earlier database version.
 *
 * Each test builds the database exactly as that version of the app left it: its tables and indexes
 * from app/schemas/<version>.json (recovered from the version's last commit), with a played game in
 * it. It then opens the database the way the app does (AppDatabase.ALL_MIGRATIONS). Room checks the
 * result against today's layout and throws if a migration got it wrong, which on a phone would be a
 * crash at every start after the update.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseUpgradeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val schemas = listOf("schemas", "app/schemas")
        .map { File(it, "com.pixelquest.app.data.local.AppDatabase") }
        .first { it.isDirectory }

    private fun schema(version: Int) = JSONObject(File(schemas, "$version.json").readText()).getJSONObject("database")

    private fun columns(db: JSONObject, table: String): Set<String> {
        val entities = db.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            if (entity.getString("tableName") != table) continue
            val fields = entity.getJSONArray("fields")
            return (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }.toSet()
        }
        return emptySet()
    }

    /** A played game, in whatever columns this version had. */
    private val rows: Map<String, List<Map<String, Any?>>> = mapOf(
        "user_profile" to listOf(
            mapOf("id" to 1, "username" to "Aria", "avatarId" to "avatar_knight", "level" to 4, "totalXp" to 1500,
                "perfectDaysTowardNextLevel" to 2, "createdAt" to 1_700_000_000_000L, "leaderboardOptIn" to 0)
        ),
        "streaks" to listOf(
            mapOf("id" to 1, "currentStreak" to 3, "longestStreak" to 9, "lastCompletedDate" to "2026-10-10", "perfectDaysCount" to 12)
        ),
        "difficulty_settings" to listOf(
            mapOf("id" to 1, "difficultyLevel" to "HARD", "perfectDayThreshold" to 0.9, "daysRequiredPerLevel" to 14)
        ),
        "tasks" to listOf(
            mapOf("id" to 1, "name" to "Read 15 Pages", "description" to "", "scheduledDay" to "2026-10-01", "scheduledTime" to "21:00",
                "recurrenceType" to "DAILY", "category" to "LEARNING", "isActive" to 1, "createdAt" to 1_700_000_000_000L,
                "reminderEnabled" to 1, "reminderLeadMinutes" to 0, "reminderStyle" to "STANDARD", "weeklyDays" to 0),
            // 7 October 2026 is a Wednesday: the weekday a weekly quest gets when weekdays are added.
            mapOf("id" to 2, "name" to "Gym", "description" to "Legs", "scheduledDay" to "2026-10-07", "scheduledTime" to "17:30",
                "recurrenceType" to "WEEKLY", "category" to "FITNESS", "isActive" to 1, "createdAt" to 1_700_000_000_000L,
                "reminderEnabled" to 1, "reminderLeadMinutes" to 0, "reminderStyle" to "STANDARD", "weeklyDays" to 4)
        ),
        "task_completion_logs" to listOf(
            mapOf("id" to 1, "taskId" to 1, "completedDate" to "2026-10-10", "wasCompleted" to 1, "pointsAwarded" to 50),
            // A double record of the same day (before version 7 made it impossible): the completed one stays.
            mapOf("id" to 2, "taskId" to 1, "completedDate" to "2026-10-10", "wasCompleted" to 0, "pointsAwarded" to 0),
            mapOf("id" to 3, "taskId" to 2, "completedDate" to "2026-10-07", "wasCompleted" to 1, "pointsAwarded" to 60)
        ),
        "level_history" to listOf(
            mapOf("id" to 1, "level" to 2, "achievedDate" to 1_700_100_000_000L, "difficultyAtTimeOfLevelUp" to "MEDIUM")
        ),
        // From version 8: a prayer marked, with the XP it was given (and from version 9, the all-five
        // bonus it unlocked).
        "namaz_log" to listOf(
            mapOf("id" to 1, "date" to "2026-10-17", "prayer" to "FAJR", "status" to "PRAYED", "prayedAt" to 1_760_000_000_000L,
                "xpAwarded" to 10, "bonusXp" to 30)
        ),
        // From version 10: a Darood Party in the cache, with a member (version 11 adds when their row changed).
        "darood_party" to listOf(
            mapOf("id" to "p1", "name" to "Friday circle", "leaderId" to "u1", "maxMembers" to 10, "milestone" to null,
                "milestoneSetAt" to null, "completedAt" to null, "createdAt" to 1_760_000_000_000L, "total" to 313,
                "entryCount" to 1, "memberCount" to 1, "lastEntryDate" to "2026-10-19", "myRole" to "LEADER",
                "myStatus" to "ACTIVE", "syncedAt" to 1_760_000_000_000L)
        ),
        "darood_party_member" to listOf(
            mapOf("partyId" to "p1", "userId" to "u1", "username" to "aria_k", "role" to "LEADER", "status" to "ACTIVE",
                "invitedAt" to null, "joinedAt" to 1_760_000_000_000L, "total" to 313, "activeDays" to 1, "lastEntryDate" to "2026-10-19")
        ),
        // From version 8: the tracker turned on, in Lahore, with Fajr's reminder off.
        "namaz_settings" to listOf(
            mapOf("id" to 1, "enabled" to 1, "method" to "KARACHI", "asrMadhab" to "HANAFI", "highLatitudeRule" to null,
                "latitude" to 31.5204, "longitude" to 74.3587, "city" to "Lahore", "reminderOffsetMin" to 5,
                "remindFajr" to 0, "remindDhuhr" to 1, "remindAsr" to 1, "remindMaghrib" to 1, "remindIsha" to 1,
                "adjustFajr" to 2, "adjustDhuhr" to 0, "adjustAsr" to 0, "adjustMaghrib" to 0, "adjustIsha" to 0, "xpEnabled" to 1)
        )
    )

    /** Creates the file exactly as [version] of the app left it, with [rows] in it. */
    private fun createAt(version: Int, name: String) {
        context.deleteDatabase(name)
        val db = schema(version)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(sqlite: SupportSQLiteDatabase) {
                        val entities = db.getJSONArray("entities")
                        for (i in 0 until entities.length()) {
                            val entity = entities.getJSONObject(i)
                            val table = entity.getString("tableName")
                            sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                            val indices = entity.optJSONArray("indices") ?: continue
                            for (j in 0 until indices.length()) {
                                sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                            }
                        }
                        val setup = db.getJSONArray("setupQueries")
                        for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
                    }

                    override fun onUpgrade(sqlite: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        val sqlite = helper.writableDatabase
        rows.forEach { (table, tableRows) ->
            val present = columns(db, table)
            if (present.isEmpty()) return@forEach
            tableRows.forEach { row ->
                val kept = row.filterKeys { it in present }
                // From version 7 a quest can't have two results on one day (its unique index), so a file
                // of that age never held the double record; it's left out (the completed one is first).
                val verb = if (version >= 7) "INSERT OR IGNORE" else "INSERT"
                val sql = "$verb INTO `$table` (${kept.keys.joinToString { "`$it`" }}) VALUES (${kept.keys.joinToString { "?" }})"
                sqlite.execSQL(sql, kept.values.toTypedArray())
            }
        }
        helper.close()
    }

    private fun openLikeTheApp(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    private fun updateFrom(version: Int) = runBlocking {
        val name = "upgrade-from-$version.db"
        createAt(version, name)
        val db = openLikeTheApp(name)
        try {
            // Opening runs the migrations and checks the layout (it throws on a mismatch).
            db.openHelper.writableDatabase

            val profile = db.userProfileDao().getProfile().first()!!
            assertEquals("Aria", profile.username)
            assertEquals(4, profile.level)
            assertEquals(1500, profile.totalXp)
            assertEquals(false, profile.leaderboardOptIn)

            val streak = db.streakDao().getCurrentStreak().first()!!
            assertEquals(3, streak.currentStreak)
            assertEquals(9, streak.longestStreak)
            assertEquals(LocalDate.of(2026, 10, 10), streak.lastCompletedDate)

            val tasks = db.taskDao().getAllTasks().first().associateBy { it.id }
            assertEquals(setOf(1L, 2L), tasks.keys)
            val gym = tasks.getValue(2L)
            assertEquals("a weekly quest keeps repeating on its first day's weekday", setOf(DayOfWeek.WEDNESDAY), gym.weeklyDays)
            assertTrue("existing quests keep their reminders", gym.reminderEnabled)
            assertEquals(ReminderStyle.STANDARD, gym.reminderStyle)

            val logs = db.taskCompletionLogDao().getAllLogs().first()
            assertEquals("one result per quest per day", 2, logs.size)
            assertTrue("the completed one of a double record is kept", logs.single { it.taskId == 1L }.wasCompleted)

            if (version >= 2) assertEquals(listOf(2), db.levelHistoryDao().getAllHistory().first().map { it.level })

            if (version >= 8) {
                // A prayer marked keeps its XP, and its bonus from version 9 (before, the column's 0).
                val fajr = db.namazLogDao().getAll().single()
                assertEquals(10, fajr.xpAwarded)
                assertEquals(if (version >= 9) 30 else 0, fajr.bonusXp)
                // The tracker's settings come through whole.
                val settings = db.namazSettingsDao().get()!!
                assertTrue(settings.enabled)
                assertEquals("Lahore", settings.city)
                assertEquals(5, settings.reminderOffsetMin)
                assertEquals(false, settings.remindFajr)
                assertEquals(2, settings.adjustFajr)
            } else {
                // Version 8's Namaz Tracker tables are there, empty: nothing is logged until it's turned on.
                assertTrue(db.namazLogDao().getAll().isEmpty())
                assertEquals(null, db.namazSettingsDao().get())
            }

            if (version >= 10) {
                // The Darood Party cache comes through; when its member's row changed isn't known until the next refresh.
                assertEquals(listOf("Friday circle"), db.daroodDao().observeParties().first().map { it.name })
                val member = db.daroodDao().observeMembers("p1").first().single()
                assertEquals("aria_k", member.username)
                assertEquals(null, member.statusChangedAt)
                assertEquals("off the race board until the server says", false, db.daroodDao().observeParties().first().single().isPublicRace)
            } else {
                // Version 10's Darood Party cache is there, empty: it fills from the server.
                assertTrue(db.daroodDao().observeParties().first().isEmpty())
            }
            assertTrue(db.daroodDao().pendingToSend().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test fun fromVersion1() = updateFrom(1)
    @Test fun fromVersion2_theV1_0_xReleases() = updateFrom(2)
    @Test fun fromVersion3_theV1_1_0Release() = updateFrom(3)
    @Test fun fromVersion4() = updateFrom(4)
    @Test fun fromVersion5() = updateFrom(5)
    @Test fun fromVersion6() = updateFrom(6)
    @Test fun fromVersion7_theV1_2_0Release() = updateFrom(7)
    @Test fun fromVersion8_theNamazTracker() = updateFrom(8)
    @Test fun fromVersion9_theV1_3_0Release() = updateFrom(9)
    @Test fun fromVersion10_theDaroodPartyCache() = updateFrom(10)
    @Test fun fromVersion11() = updateFrom(11)

    @Test
    fun everyEarlierVersion_hasALayoutAndAMigration() {
        val current = schema(AppDatabase.ALL_MIGRATIONS.last().endVersion).getInt("version")
        assertEquals(
            "a layout file per version, 1 to $current: bump the version, and its file is written at the next build",
            (1..current).map { "$it.json" }.toSet(),
            schemas.list()!!.filter { it.endsWith(".json") }.toSet()
        )
        assertEquals(
            "a migration from each version to the next",
            (1 until current).map { it to it + 1 },
            AppDatabase.ALL_MIGRATIONS.map { it.startVersion to it.endVersion }
        )
    }

    @Test
    fun anUpdateNeverStartsOver() {
        val module = listOf("src/main/java", "app/src/main/java")
            .map { File(it, "com/pixelquest/app/di/DatabaseModule.kt") }
            .first { it.exists() }
            .readLines()
            .filterNot { it.trim().startsWith("//") }
            .joinToString("\n")
        assertTrue("no wipe on a missing update path", "fallbackToDestructiveMigration()" !in module)
        assertTrue(".fallbackToDestructiveMigrationOnDowngrade()" in module)
    }

    @Test
    fun theAppKeepsACopyOfTheOldFile_beforeUpdatingIt(): Unit = runBlocking {
        // The 1.1.0 release's file, opened the way the app opens it.
        val name = com.pixelquest.app.di.DatabaseModule.DATABASE_NAME
        createAt(3, name)
        val copies = File(context.filesDir, PreUpdateCopy.DIR_NAME).apply { deleteRecursively() }
        lateinit var db: AppDatabase
        db = com.pixelquest.app.di.DatabaseModule.provideAppDatabase(context) { db }
        try {
            db.openHelper.writableDatabase
            assertEquals("Aria", db.userProfileDao().getProfile().first()!!.username)
        } finally {
            db.close()
        }

        val copy = File(copies, "pixelquest-v3.db")
        assertEquals("the old file, as 1.1.0 left it", 3, PreUpdateCopy.versionOf(copy))
        assertEquals("and the updated one", AppDatabase.ALL_MIGRATIONS.last().endVersion, PreUpdateCopy.versionOf(context.getDatabasePath(name)))
        context.deleteDatabase(name)
        copies.deleteRecursively()
    }

    @Test
    fun aNamazLogRow_isOnePerPrayerPerDay_afterTheUpdate(): Unit = runBlocking {
        val name = "upgrade-namaz-unique.db"
        createAt(7, name)
        val db = openLikeTheApp(name)
        try {
            val day = LocalDate.of(2026, 10, 17)
            db.namazLogDao().upsert(com.pixelquest.app.data.local.entity.NamazLogEntity(date = day, prayer = com.pixelquest.app.domain.namaz.Prayer.FAJR, status = com.pixelquest.app.domain.namaz.PrayerStatus.PENDING))
            db.namazLogDao().upsert(com.pixelquest.app.data.local.entity.NamazLogEntity(date = day, prayer = com.pixelquest.app.domain.namaz.Prayer.FAJR, status = com.pixelquest.app.domain.namaz.PrayerStatus.PRAYED, prayedAt = 1L))

            val rows = db.namazLogDao().getForDate(day)
            assertEquals("the migration's unique index keeps one row per prayer per day", 1, rows.size)
            assertEquals(com.pixelquest.app.domain.namaz.PrayerStatus.PRAYED, rows.single().status)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
