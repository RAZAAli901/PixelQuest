package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.components.Pixel7DayHistoryStrip
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.screens.stats.StatsViewModel
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val longestStreak = state.streak?.longestStreak ?: 0
    val currentStreak = state.streak?.currentStreak ?: 0
    val activeDifficulty = state.difficulty?.difficultyLevel ?: DifficultyLevel.MEDIUM

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📊 HERO STATISTICS",
            style = PixelTypography.titleLarge,
            color = PixelGold
        )

        // Longest Streak Card
        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏆",
                    style = PixelTypography.displayMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LONGEST STREAK",
                        style = PixelTypography.labelLarge,
                        color = PixelGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$longestStreak DAYS (Current: $currentStreak)",
                        style = PixelTypography.bodyMedium,
                        color = PixelTextWhite
                    )
                }
            }
        }

        // Active Difficulty Card
        PixelCard(
            variant = PixelPanelVariant.BLUE,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🛡️",
                    style = PixelTypography.displayMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE DIFFICULTY",
                        style = PixelTypography.labelLarge,
                        color = PixelCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DifficultyMode.getDisplayName(activeDifficulty).uppercase(),
                        style = PixelTypography.titleMedium,
                        color = PixelGreen
                    )
                    Text(
                        text = "${(DifficultyMode.getPerfectDayThreshold(activeDifficulty) * 100).toInt()}% Target Threshold",
                        style = PixelTypography.bodySmall,
                        color = PixelTextWhite
                    )
                }
            }
        }

        // 7-Day History Strip
        Pixel7DayHistoryStrip(days = state.last7DaysLogs)
    }
}

