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
 * A paused quest gets no alarm, whoever re-arms. Switching notifications back on in Settings (and
 * every boot) armed every quest, paused ones included, and their alarms woke the device for nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PausedQuestNotArmedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheduler = TaskAlarmScheduler(context)
    private val alarms get() = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms

    private fun quest(id: Long, active: Boolean) = TaskEntity(
        id = id, name = "Quest $id", description = "", scheduledDay = LocalDate.now().minusDays(1),
        scheduledTime = LocalTime.of(23, 59), recurrenceType = RecurrenceType.DAILY, category = TaskCategory.HEALTH,
        isActive = active
    )

    private fun armedIds() = alarms.map { shadowOf(it.operation).savedIntent.getLongExtra("EXTRA_TASK_ID", -1L) }

    @Test
    fun reArmingEveryQuest_skipsThePausedOnes() {
        scheduler.rescheduleAllAlarms(listOf(quest(1, active = true), quest(2, active = false)))

        assertEquals(listOf(1L), armedIds())
    }

    @Test
    fun pausingAQuest_clearsTheAlarmItHad() {
        scheduler.scheduleExactAlarmForTask(quest(3, active = true))
        assertEquals(listOf(3L), armedIds())

        scheduler.scheduleExactAlarmForTask(quest(3, active = false)) // what saving the edited quest does

        assertEquals(emptyList<Long>(), armedIds())
    }
}
