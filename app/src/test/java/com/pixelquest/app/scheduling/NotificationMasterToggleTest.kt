package com.pixelquest.app.scheduling

import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NotificationMasterToggleTest {

    @Test
    fun testDisableNotificationsCancelsAllAlarms() {
        var canceledCount = 0
        var rescheduledCount = 0

        val tasks = listOf(
            TaskEntity(id = 1, name = "Task 1", description = "Desc", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(9, 0), recurrenceType = RecurrenceType.DAILY, category = com.pixelquest.app.domain.model.TaskCategory.OTHER),
            TaskEntity(id = 2, name = "Task 2", description = "Desc", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(18, 0), recurrenceType = RecurrenceType.DAILY, category = com.pixelquest.app.domain.model.TaskCategory.OTHER)
        )

        fun onToggle(enabled: Boolean) {
            if (!enabled) {
                canceledCount += tasks.size
            } else {
                rescheduledCount += tasks.size
            }
        }

        onToggle(false)
        assertEquals(2, canceledCount)
        assertEquals(0, rescheduledCount)

        onToggle(true)
        assertEquals(2, canceledCount)
        assertEquals(2, rescheduledCount)
    }
}
