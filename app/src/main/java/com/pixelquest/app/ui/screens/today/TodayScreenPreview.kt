package com.pixelquest.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.ui.components.TaskItemStatus
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime

@Preview(name = "Today Screen - Gamified Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun TodayScreenGamifiedPreview() {
    PixelQuestTheme {
        val today = LocalDate.now()
        val sampleTasks = listOf(
            TodayTaskItem(
                task = TaskEntity(id = 1, name = "Morning Gym Session", description = "Morning workout routine", scheduledTime = LocalTime.of(9, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.PENDING
            ),
            TodayTaskItem(
                task = TaskEntity(id = 2, name = "Kotlin Documentation", description = "Read coroutines and flows", scheduledTime = LocalTime.of(14, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.PENDING
            ),
            TodayTaskItem(
                task = TaskEntity(id = 3, name = "Drink 2L Water", description = "Daily hydration goal", scheduledTime = LocalTime.of(7, 30), scheduledDay = today, category = TaskCategory.HEALTH, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.DONE
            ),
            TodayTaskItem(
                task = TaskEntity(id = 4, name = "Take Out Trash", description = "Take trash bins outside", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.MISSED
            )
        )

        val state = TodayUiState.Success(
            tasks = sampleTasks,
            currentStreak = 7,
            totalXp = 850,
            level = 4,
            perfectDaysTowardNextLevel = 3,
            daysRequiredPerLevel = 7,
            completionPercentage = 0.5f,
            targetThreshold = 0.7f,
            isPerfectDay = false,
            flavorText = "Halfway there, hero! Keep up the momentum!",
            isSimpleMode = false
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
        ) {
            TodayContent(
                state = state,
                onQuickComplete = {},
                onQuickSkip = {},
                onRefresh = {},
                onCreateQuestClick = {},
                onNavigateToEditTask = {},
                onNavigateToProfile = {}
            )
        }
    }
}

@Preview(name = "Today Screen - Simple Mode", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun TodayScreenSimpleModePreview() {
    PixelQuestTheme {
        val today = LocalDate.now()
        val sampleTasks = listOf(
            TodayTaskItem(
                task = TaskEntity(id = 1, name = "Morning Gym Session", description = "Morning workout routine", scheduledTime = LocalTime.of(9, 0), scheduledDay = today, category = TaskCategory.FITNESS, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.PENDING
            ),
            TodayTaskItem(
                task = TaskEntity(id = 2, name = "Kotlin Documentation", description = "Read coroutines and flows", scheduledTime = LocalTime.of(14, 0), scheduledDay = today, category = TaskCategory.LEARNING, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.PENDING
            ),
            TodayTaskItem(
                task = TaskEntity(id = 3, name = "Drink 2L Water", description = "Daily hydration goal", scheduledTime = LocalTime.of(7, 30), scheduledDay = today, category = TaskCategory.HEALTH, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.DONE
            ),
            TodayTaskItem(
                task = TaskEntity(id = 4, name = "Take Out Trash", description = "Take trash bins outside", scheduledTime = LocalTime.of(8, 0), scheduledDay = today, category = TaskCategory.CHORES, recurrenceType = RecurrenceType.DAILY),
                status = TaskItemStatus.MISSED
            )
        )

        val state = TodayUiState.Success(
            tasks = sampleTasks,
            currentStreak = 7,
            totalXp = 850,
            level = 4,
            perfectDaysTowardNextLevel = 3,
            daysRequiredPerLevel = 7,
            completionPercentage = 0.5f,
            targetThreshold = 0.7f,
            isPerfectDay = false,
            flavorText = "Tasks in progress. Keep moving forward.",
            isSimpleMode = true
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
        ) {
            TodayContent(
                state = state,
                onQuickComplete = {},
                onQuickSkip = {},
                onRefresh = {},
                onCreateQuestClick = {},
                onNavigateToEditTask = {},
                onNavigateToProfile = {}
            )
        }
    }
}

@Preview(name = "Today Screen - Gamified vs Simple Side by Side", showBackground = true, widthDp = 760, heightDp = 740)
@Composable
fun TodayScreenComparisonSideBySidePreview() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            TodayScreenGamifiedPreview()
        }
        Box(modifier = Modifier.weight(1f)) {
            TodayScreenSimpleModePreview()
        }
    }
}
