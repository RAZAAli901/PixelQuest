package com.pixelquest.app.data.local

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.domain.model.ReminderStyle
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.notification.NotificationContentBuilder
import com.pixelquest.app.notification.ReminderContext
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
class Migration4To5Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // The tasks table exactly as version 4 created it.
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `scheduledDay` TEXT NOT NULL, " +
                            "`scheduledTime` TEXT NOT NULL, `recurrenceType` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                            "`isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
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

    @Test
    fun migration_keepsExistingTasks_withDefaultReminderSettings() {
        db.execSQL(
            "INSERT INTO tasks (name, description, scheduledDay, scheduledTime, recurrenceType, category, isActive, createdAt) " +
                "VALUES ('Go to Gym', '', '2026-10-01', '17:30', 'DAILY', 'FITNESS', 1, 0)"
        )

        AppDatabase.MIGRATION_4_5.migrate(db)

        db.query("SELECT name, reminderEnabled, reminderLeadMinutes, reminderStyle FROM tasks").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Go to Gym", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals(0, c.getInt(2))
            assertEquals("STANDARD", c.getString(3))
        }
    }

    @Test
    fun converter_unknownStyle_fallsBackToStandard() {
        val converters = Converters()
        assertEquals(ReminderStyle.PROMPT, converters.toReminderStyle("PROMPT"))
        assertEquals(ReminderStyle.STANDARD, converters.toReminderStyle("LOUD"))
        assertEquals("SILENT", converters.fromReminderStyle(ReminderStyle.SILENT))
    }

    @Test
    fun leadTimeCopy_mentionsMinutesInBothModes() {
        val base = ReminderContext(
            taskName = "Go to Gym",
            category = TaskCategory.FITNESS,
            isSimpleMode = false,
            currentStreak = 0,
            doneToday = 0,
            totalToday = 1,
            startsInMinutes = 15
        )
        assertEquals("💪 Quest in 15 min: Go to Gym", NotificationContentBuilder.reminder(base).title)
        assertEquals("Coming up in 15 min: Go to Gym", NotificationContentBuilder.reminder(base.copy(isSimpleMode = true)).title)
    }
}
