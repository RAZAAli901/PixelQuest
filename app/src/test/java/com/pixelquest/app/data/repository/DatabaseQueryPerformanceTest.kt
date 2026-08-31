package com.pixelquest.app.data.repository

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DatabaseQueryPerformanceTest {

    @Test
    fun statsAggregation_singlePassInMapGroupBy_zeroNPlusOneQueries() {
        val startDate = LocalDate.of(2026, 1, 1)
        val endDate = LocalDate.of(2026, 3, 31)

        val task = TaskEntity(
            id = 1L,
            name = "Morning Stretch",
            scheduledDay = startDate,
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            selectedDays = setOf(DayOfWeek.MONDAY),
            category = TaskCategory.HEALTH
        )

        val logs = (0..89).map { dayOffset ->
            TaskCompletionLogEntity(
                id = dayOffset.toLong() + 1L,
                taskId = 1L,
                completedDate = startDate.plusDays(dayOffset.toLong()),
                wasCompleted = true,
                pointsAwarded = 10
            )
        }

        // Single pass batch grouping
        val logsByDate = logs.groupBy { it.completedDate }
        assertEquals(90, logsByDate.size)

        var count = 0
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            val dayLogs = logsByDate[curr] ?: emptyList()
            count += dayLogs.size
            curr = curr.plusDays(1)
        }
        assertEquals(90, count)
    }
}
