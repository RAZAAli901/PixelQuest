package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The AI Coach's "Recent Consistency" covers today and the 6 days before it, not all history. */
class RecentConsistencyWindowTest {

    private val today = LocalDate.of(2026, 10, 3)

    private fun log(daysAgo: Long, done: Boolean) = TaskCompletionLogEntity(
        taskId = 1, completedDate = today.minusDays(daysAgo), wasCompleted = done, pointsAwarded = if (done) 50 else 0
    )

    @Test
    fun olderLogs_areLeftOutOfTheRecentNumbers() {
        // A bad month long ago, then a perfect last week.
        val logs = (10L..30L).map { log(it, done = false) } + (0L..6L).map { log(it, done = true) }

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(null, null, emptyList(), logs, today)

        assertEquals(1f, telemetry.recent7DaysCompletionRate, 0.0001f)
        assertEquals(0, telemetry.recent7DaysMissedCount)
    }

    @Test
    fun theWindow_isExactlySevenDays() {
        val logs = listOf(log(6, done = false), log(7, done = false), log(0, done = true))

        val telemetry = HabitInsightPromptBuilder.buildTelemetrySummary(null, null, emptyList(), logs, today)

        assertEquals(1, telemetry.recent7DaysMissedCount) // 6 days ago counts, 7 days ago doesn't
        assertEquals(0.5f, telemetry.recent7DaysCompletionRate, 0.0001f)
    }
}
