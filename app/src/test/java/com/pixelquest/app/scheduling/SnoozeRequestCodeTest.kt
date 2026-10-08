package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Quest 1's snooze (request code 1 * 10 + 4 = 14) and quest 14's reminder (code 14) used to be the
 * same PendingIntent: snoozing quest 1 replaced quest 14's reminder, and finishing quest 1 cancelled it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SnoozeRequestCodeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = TaskAlarmScheduler(context)

    private val quest14 = TaskEntity(
        id = 14, name = "Read", description = "", scheduledDay = LocalDate.now().minusDays(1),
        scheduledTime = LocalTime.of(21, 0), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.LEARNING
    )

    private fun armedTaskIds() = alarms.scheduledAlarms.map { shadowOf(it.operation).savedIntent.getLongExtra("EXTRA_TASK_ID", -1L) }

    @Test
    fun snoozingQuest1_keepsQuest14sReminder() {
        scheduler.scheduleExactAlarmForTask(quest14)
        assertEquals(listOf(14L), armedTaskIds())

        scheduler.scheduleSnooze(taskId = 1, taskName = "Run")

        assertEquals(setOf(1L, 14L), armedTaskIds().toSet())
    }

    @Test
    fun finishingQuest1_cancelsOnlyItsOwnSnooze() {
        scheduler.scheduleExactAlarmForTask(quest14)
        scheduler.scheduleSnooze(taskId = 1, taskName = "Run")

        scheduler.clearReminder(1)

        assertEquals(listOf(14L), armedTaskIds())
    }

    @Test
    fun rearmingQuest14_keepsQuest1sSnooze() {
        scheduler.scheduleSnooze(taskId = 1, taskName = "Run")

        scheduler.scheduleExactAlarmForTask(quest14)

        assertTrue(1L in armedTaskIds())
        assertTrue(14L in armedTaskIds())
    }
}
