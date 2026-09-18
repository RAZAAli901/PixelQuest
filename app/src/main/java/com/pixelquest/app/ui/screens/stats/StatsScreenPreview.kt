package com.pixelquest.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.screens.StatsContent
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelQuestTheme
import java.time.LocalDate

@Preview(name = "Stats Screen - Gamified Mode", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun StatsScreenGamifiedPreview() {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
        ) {
            StatsContent(
                state = StatsUiState(
                    currentStreak = 7,
                    longestStreak = 14,
                    totalPoints = 1250,
                    overallCompletionRate = 0.85f,
                    difficultyLevel = DifficultyLevel.HARD,
                    heatmapStatusMap = sampleMap,
                    weeklyTrend = listOf("W-3" to 0.7f, "W-2" to 0.85f, "W-1" to 0.6f, "NOW" to 0.9f),
                    isSimpleMode = false
                )
            )
        }
    }
}

@Preview(name = "Stats Screen - Simple Mode", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun StatsScreenSimpleModePreview() {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelBackgroundDark)
        ) {
            StatsContent(
                state = StatsUiState(
                    currentStreak = 7,
                    longestStreak = 14,
                    totalPoints = 1250,
                    overallCompletionRate = 0.85f,
                    difficultyLevel = DifficultyLevel.HARD,
                    heatmapStatusMap = sampleMap,
                    weeklyTrend = listOf("W-3" to 0.7f, "W-2" to 0.85f, "W-1" to 0.6f, "NOW" to 0.9f),
                    isSimpleMode = true
                )
            )
        }
    }
}

@Preview(name = "Stats Screen - Gamified vs Simple Side by Side", showBackground = true, widthDp = 760, heightDp = 800)
@Composable
fun StatsScreenComparisonSideBySidePreview() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            StatsScreenGamifiedPreview()
        }
        Box(modifier = Modifier.weight(1f)) {
            StatsScreenSimpleModePreview()
        }
    }
}
