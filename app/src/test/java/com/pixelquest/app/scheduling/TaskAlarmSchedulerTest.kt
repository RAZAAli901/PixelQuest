package com.pixelquest.app.scheduling

import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskAlarmSchedulerTest {

    private lateinit var scheduler: TaskAlarmScheduler

    @Before
    fun setUp() {
        scheduler = TaskAlarmScheduler(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun calculateNextOccurrenceDate_daily_returnsTomorrow() {
        val today = LocalDate.of(2026, 8, 19)
        val task = TaskEntity(
            id = 1,
            name = "Test Task",
            description = "",
            scheduledDay = today,
            scheduledTime = LocalTime.of(10, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS
        )

        val nextDate = scheduler.calculateNextOccurrenceDate(task, today)
        assertEquals(LocalDate.of(2026, 8, 20), nextDate)
    }

    @Test
    fun calculateTriggerTimeMillis_returnsValidTimestamp() {
        val task = TaskEntity(
            id = 1,
            name = "Test Task",
            description = "",
            scheduledDay = LocalDate.now(),
            scheduledTime = LocalTime.of(23, 59),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS
        )

        val triggerTime = scheduler.calculateTriggerTimeMillis(task)
        assertTrue(triggerTime > System.currentTimeMillis())
    }
}
