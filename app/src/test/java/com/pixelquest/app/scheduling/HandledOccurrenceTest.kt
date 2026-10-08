package com.pixelquest.app.scheduling

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Once today's occurrence has been reminded (or done, or skipped), re-arming never goes back to it.
 * A cold start inside a quest's lead window used to arm today's reminder again at the task time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HandledOccurrenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.now()
    private val scheduler = TaskAlarmScheduler(context)

    // Late in the day so that today's occurrence is still ahead whenever the test runs.
    private val quest = TaskEntity(
        id = 8, name = "Stretch", description = "", scheduledDay = today.minusDays(3), scheduledTime = LocalTime.of(23, 59),
        recurrenceType = RecurrenceType.DAILY, category = TaskCategory.HEALTH
    )

    private fun armedDay(): LocalDate? = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
        .single { shadowOf(it.operation).savedIntent.getLongExtra("EXTRA_TASK_ID", -1L) == quest.id }
        .let { TaskAlarmScheduler.occurrenceDateFrom(shadowOf(it.operation).savedIntent) }

    @Test
    fun afterTodaysReminder_reArmingOnLaunch_waitsForTomorrow() {
        scheduler.scheduleNextOccurrence(quest, handledDate = today) // what the receiver does once it has reminded

        scheduler.rescheduleAllAlarms(listOf(quest)) // the app's launch-time re-arm

        assertEquals(today.plusDays(1), armedDay())
        assertEquals(today, scheduler.lastHandled(quest.id))
    }

    @Test
    fun withNothingHandledYet_todayIsArmed() {
        scheduler.scheduleExactAlarmForTask(quest)

        assertEquals(today, armedDay())
    }

    @Test
    fun editingTheQuest_allowsTodayAgain() {
        scheduler.scheduleNextOccurrence(quest, handledDate = today)

        scheduler.forgetHandled(quest.id)
        scheduler.scheduleExactAlarmForTask(quest)

        assertEquals(today, armedDay())
    }
}
