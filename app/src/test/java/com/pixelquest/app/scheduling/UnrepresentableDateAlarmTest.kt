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

/** A quest dated in the year 999999999 used to throw while arming alarms, which crashed boot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UnrepresentableDateAlarmTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun anUnrepresentableDate_isSkipped_andTheOtherQuestsStillGetTheirReminders() {
        val scheduler = TaskAlarmScheduler(context)
        val farFuture = TaskEntity(
            id = 1, name = "Far", description = "", scheduledDay = LocalDate.of(999_999_999, 1, 1),
            scheduledTime = LocalTime.of(9, 0), recurrenceType = RecurrenceType.ONE_TIME, category = TaskCategory.FITNESS
        )
        val normal = farFuture.copy(id = 2, name = "Run", scheduledDay = LocalDate.now().minusDays(1), recurrenceType = RecurrenceType.DAILY)

        scheduler.rescheduleAllAlarms(listOf(farFuture, normal))

        val armed = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
            .map { shadowOf(it.operation).savedIntent.getLongExtra("EXTRA_TASK_ID", -1L) }
        assertEquals(listOf(2L), armed)
    }
}
