package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Each reminder carries the day of the quest occurrence it's for. The receiver used to work it out
 * from when the alarm fired, which went wrong after midnight (a snooze or a late inexact alarm).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReminderOccurrenceDateTest {

    private val sunday = LocalDate.of(2026, 10, 11)
    private val monday = sunday.plusDays(1)

    private fun mondayQuest(lead: Int) = ReminderSchedule.nextReminder(
        scheduledDay = monday.minusWeeks(2), scheduledTime = LocalTime.of(0, 10), recurrence = RecurrenceType.WEEKLY,
        now = LocalDateTime.of(sunday, LocalTime.of(20, 0)), leadMinutes = lead, weeklyDays = setOf(DayOfWeek.MONDAY)
    )!!

    @Test
    fun aReminderTheEveningBefore_isForTheNextDay() {
        val next = mondayQuest(lead = 30)

        assertEquals(LocalDateTime.of(sunday, LocalTime.of(23, 40)), next.at)
        assertEquals(monday, next.occurrenceDate)
    }

    @Test
    fun whenTheLeadWindowHasStarted_itRemindsAtTheTaskTime_forThatDay() {
        val next = ReminderSchedule.nextReminder(
            scheduledDay = sunday.minusDays(3), scheduledTime = LocalTime.of(23, 30), recurrence = RecurrenceType.DAILY,
            now = LocalDateTime.of(sunday, LocalTime.of(23, 0)), leadMinutes = 60
        )!!

        assertEquals(LocalDateTime.of(sunday, LocalTime.of(23, 30)), next.at)
        assertEquals(sunday, next.occurrenceDate)
    }

    @Test
    fun theArmedAlarm_carriesTheDay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val today = LocalDate.now()
        val task = TaskEntity(
            id = 5, name = "Run", description = "", scheduledDay = today.minusDays(2), scheduledTime = LocalTime.of(23, 59),
            recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
        )

        TaskAlarmScheduler(context).scheduleExactAlarmForTask(task, notBefore = today.plusDays(1))

        val intent = shadowOf(shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.single().operation).savedIntent
        assertEquals(today.plusDays(1), TaskAlarmScheduler.occurrenceDateFrom(intent))
    }

    @Test
    fun anAlarmFromBeforeDay31_hasNoDay() {
        assertNull(TaskAlarmScheduler.occurrenceDateFrom(Intent().putExtra("EXTRA_TASK_ID", 5L)))
        assertNull(TaskAlarmScheduler.occurrenceDateFrom(null))
    }
}
