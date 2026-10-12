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
                val sql = "INSERT INTO `$table` (${kept.keys.joinToString { "`$it`" }}) VALUES (${kept.keys.joinToString { "?" }})"
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

    @Test
    fun everyEarlierVersion_hasALayoutAndAMigration() {
        val current = schema(7).getInt("version")
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
}

