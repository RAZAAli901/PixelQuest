package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.WeeklyDays
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class Migration5To6Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // The tasks table exactly as version 5 created it.
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `scheduledDay` TEXT NOT NULL, " +
                            "`scheduledTime` TEXT NOT NULL, `recurrenceType` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                            "`isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                            "`reminderEnabled` INTEGER NOT NULL DEFAULT 1, `reminderLeadMinutes` INTEGER NOT NULL DEFAULT 0, " +
                            "`reminderStyle` TEXT NOT NULL DEFAULT 'STANDARD')"
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

    private fun insert(name: String, day: String, recurrence: String) {
        db.execSQL(
            "INSERT INTO tasks (name, description, scheduledDay, scheduledTime, recurrenceType, category, isActive, createdAt) " +
                "VALUES ('$name', '', '$day', '08:00', '$recurrence', 'FITNESS', 1, 0)"
        )
    }

    private fun weeklyDaysOf(name: String): Set<DayOfWeek> =
        db.query("SELECT weeklyDays FROM tasks WHERE name = '$name'").use { c ->
            c.moveToFirst()
            WeeklyDays.fromMask(c.getInt(0))
        }

    @Test
    fun weeklyTasks_keepTheirFirstDatesWeekday() {
        insert("Monday swim", "2026-09-07", "WEEKLY")    // Monday
        insert("Thursday call", "2026-10-01", "WEEKLY")  // Thursday
        insert("Sunday plan", "2026-10-04", "WEEKLY")    // Sunday

        AppDatabase.MIGRATION_5_6.migrate(db)

        assertEquals(setOf(DayOfWeek.MONDAY), weeklyDaysOf("Monday swim"))
        assertEquals(setOf(DayOfWeek.THURSDAY), weeklyDaysOf("Thursday call"))
        assertEquals(setOf(DayOfWeek.SUNDAY), weeklyDaysOf("Sunday plan"))
    }

    @Test
    fun otherTasks_getNoWeeklyDays() {
        insert("Push-ups", "2026-10-01", "DAILY")
        insert("Dentist", "2026-10-01", "ONE_TIME")

        AppDatabase.MIGRATION_5_6.migrate(db)

        assertEquals(emptySet<DayOfWeek>(), weeklyDaysOf("Push-ups"))
        assertEquals(emptySet<DayOfWeek>(), weeklyDaysOf("Dentist"))
    }

    @Test
    fun converter_roundTripsEveryCombination() {
        val converters = Converters()
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        assertEquals(0b1010001, converters.fromWeeklyDays(days))
        assertEquals(days, converters.toWeeklyDays(converters.fromWeeklyDays(days)))
        assertEquals(emptySet<DayOfWeek>(), converters.toWeeklyDays(0))
        assertEquals(DayOfWeek.values().toSet(), converters.toWeeklyDays(0b1111111))
    }
}
