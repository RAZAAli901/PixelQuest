package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The AI Coach's "Recent Consistency" is completed over scheduled quests for today and the 6 days
 * before it: older history is left out, and a quest nobody logged counts as not done.
 */
class RecentConsistencyWindowTest {

    private val today = LocalDate.of(2026, 10, 3)

    /** One daily quest that started well before the window. */
    private val dailyQuest = listOf(
        TaskEntity(
            id = 1, name = "Run", description = "", scheduledDay = today.minusDays(60), scheduledTime = LocalTime.of(7, 0),
            recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
        )
    )

    private fun log(daysAgo: Long, done: Boolean) = TaskCompletionLogEntity(
        taskId = 1, completedDate = today.minusDays(daysAgo), wasCompleted = done, pointsAwarded = if (done) 50 else 0
    )

    private fun telemetry(logs: List<TaskCompletionLogEntity>) =
        HabitInsightPromptBuilder.buildTelemetrySummary(null, null, dailyQuest, logs, today)

    @Test
    fun olderLogs_areLeftOutOfTheRecentNumbers() {
        // A bad month long ago, then a perfect last week.
        val result = telemetry((10L..30L).map { log(it, done = false) } + (0L..6L).map { log(it, done = true) })

        assertEquals(1f, result.recent7DaysCompletionRate, 0.0001f)
        assertEquals(0, result.recent7DaysMissedCount)
    }

    @Test
    fun theWindow_isExactlySevenDays() {
        // Done 7 days ago (outside), 6 days ago and today (inside): 2 of the 7 due quests.
        val result = telemetry(listOf(log(7, done = true), log(6, done = true), log(0, done = true)))

        assertEquals(2f / 7f, result.recent7DaysCompletionRate, 0.0001f)
        assertEquals(5, result.recent7DaysMissedCount)
    }

    @Test
    fun questsNobodyLogged_countAsNotDone() {
        // One completion 5 days ago; the other six days have no log at all. Used to read as 100%.
        val result = telemetry(listOf(log(5, done = true)))

        // Today has no result yet, so it isn't counted against the player: 1 of 6.
        assertEquals(1f / 6f, result.recent7DaysCompletionRate, 0.0001f)
        assertEquals(5, result.recent7DaysMissedCount)
    }
}
