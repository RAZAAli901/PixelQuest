package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
class Migration6To7Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // The logs table exactly as version 6 created it (no index).
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `task_completion_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`taskId` INTEGER NOT NULL, `completedDate` TEXT NOT NULL, `wasCompleted` INTEGER NOT NULL, " +
                            "`pointsAwarded` INTEGER NOT NULL)"
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

    private fun log(taskId: Long, date: String, completed: Boolean, points: Int) {
        db.execSQL(
            "INSERT INTO task_completion_logs (taskId, completedDate, wasCompleted, pointsAwarded) " +
                "VALUES ($taskId, '$date', ${if (completed) 1 else 0}, $points)"
        )
    }

    /** (taskId, date, wasCompleted, points) for every remaining row, in a stable order. */
    private fun rows(): List<List<Any>> =
        db.query("SELECT taskId, completedDate, wasCompleted, pointsAwarded FROM task_completion_logs ORDER BY taskId, completedDate").use { c ->
            buildList {
                while (c.moveToNext()) add(listOf(c.getLong(0), c.getString(1), c.getInt(2) == 1, c.getInt(3)))
            }
        }

    @Test
    fun duplicateCompletions_keepOneLogPerTaskAndDay() {
        log(1, "2026-10-01", completed = true, points = 50)
        log(1, "2026-10-01", completed = true, points = 60) // double tap
        log(2, "2026-10-01", completed = true, points = 50)

        AppDatabase.MIGRATION_6_7.migrate(db)

        assertEquals(
            listOf(listOf(1L, "2026-10-01", true, 60), listOf(2L, "2026-10-01", true, 50)),
            rows()
        )
    }

    @Test
    fun aCompletedLog_winsOverAMissedOne_whicheverCameFirst() {
        log(1, "2026-10-01", completed = true, points = 50)
        log(1, "2026-10-01", completed = false, points = 0)  // missed worker after completion
        log(2, "2026-10-01", completed = false, points = 0)  // "Not yet", then done later
        log(2, "2026-10-01", completed = true, points = 70)

        AppDatabase.MIGRATION_6_7.migrate(db)

        assertEquals(
            listOf(listOf(1L, "2026-10-01", true, 50), listOf(2L, "2026-10-01", true, 70)),
            rows()
        )
    }

    @Test
    fun differentDays_andSingleLogs_areKept() {
        log(1, "2026-09-30", completed = false, points = 0)
        log(1, "2026-10-01", completed = true, points = 50)
        log(1, "2026-10-02", completed = false, points = 0)
        log(1, "2026-10-02", completed = false, points = 0)

        AppDatabase.MIGRATION_6_7.migrate(db)

        assertEquals(3, rows().size)
        assertEquals(listOf(1L, "2026-10-02", false, 0), rows().last())
    }

    @Test
    fun afterMigration_aSecondLogForTheSameDay_isRejected() {
        log(1, "2026-10-01", completed = true, points = 50)
        AppDatabase.MIGRATION_6_7.migrate(db)

        val rejected = try {
            log(1, "2026-10-01", completed = true, points = 50)
            false
        } catch (e: SQLiteConstraintException) {
            true
        }
        assertTrue(rejected)
        log(1, "2026-10-02", completed = true, points = 50) // the next day is fine
        assertEquals(2, rows().size)
    }
}
