package com.pixelquest.app.ui.screens.analytics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.PerTaskStats
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.theme.PixelQuestTheme
import java.time.LocalDate
import java.time.LocalTime

@Preview(showBackground = true)
@Composable
fun TaskAnalyticsScreenPreview() {
    PixelQuestTheme {
        val sampleTask = TaskEntity(
            id = 1L,
            name = "Morning Pushups",
            description = "Daily physical training",
            scheduledDay = LocalDate.now().minusDays(14),
            scheduledTime = LocalTime.of(8, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS
        )

        val today = LocalDate.now()
        val history = (13 downTo 0).map { daysAgo ->
            Pair(today.minusDays(daysAgo.toLong()), daysAgo % 3 != 0)
        }

        val sampleStats = PerTaskStats(
            taskId = 1L,
            completionCount = 10,
            totalScheduledCount = 14,
            completionRate = 0.71f,
            currentStreak = 4,
            longestStreak = 6,
            recentHistory = history
        )

        TaskAnalyticsContent(
            task = sampleTask,
            stats = sampleStats,
            onBackClick = {}
        )
    }
}
