package com.pixelquest.app.ui.screens.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.StatsContent
import com.pixelquest.app.ui.theme.PixelQuestTheme
import java.time.LocalDate

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    PixelQuestTheme {
        val today = LocalDate.now()
        val sampleMap = (0..60).associate { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val status = when (daysAgo % 4) {
                0 -> DailyStatus.PERFECT
                1 -> DailyStatus.PARTIAL
                2 -> DailyStatus.MISSED
                else -> DailyStatus.NO_TASKS_SCHEDULED
            }
            date to status
        }

        StatsContent(
            state = StatsUiState(
                currentStreak = 7,
                longestStreak = 14,
                totalPoints = 1250,
                overallCompletionRate = 0.85f,
                difficultyLevel = DifficultyLevel.HARD,
                heatmapStatusMap = sampleMap
            )
        )
    }
}
