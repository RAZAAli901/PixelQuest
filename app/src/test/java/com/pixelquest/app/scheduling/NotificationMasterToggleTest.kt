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
            TaskEntity(1, "Task 1", "Desc", LocalTime.of(9, 0), LocalDate.now(), RecurrenceType.DAILY),
            TaskEntity(2, "Task 2", "Desc", LocalTime.of(18, 0), LocalDate.now(), RecurrenceType.DAILY)
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
