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
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.components.PixelXpBar
import com.pixelquest.app.ui.screens.home.HomeViewModel
import com.pixelquest.app.ui.theme.PixelBackgroundDark
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val streakCount = state.streak?.currentStreak ?: 0
    val totalXp = state.profile?.totalXp ?: 0
    val username = state.profile?.username ?: "PixelHero"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBackgroundDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚔️ WELCOME BACK, $username",
            style = PixelTypography.titleLarge,
            color = PixelGold
        )

        // Streak Card with Flame Icon
        PixelCard(
            variant = PixelPanelVariant.YELLOW,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔥",
                    style = PixelTypography.displayMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CURRENT STREAK",
                        style = PixelTypography.labelLarge,
                        color = PixelGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$streakCount DAYS",
                        style = PixelTypography.displaySmall,
                        color = PixelTextWhite
                    )
                }
            }
        }

        // XP / Points Card
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
                    text = "✨",
                    style = PixelTypography.displayMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL POINTS",
                        style = PixelTypography.labelLarge,
                        color = PixelCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalXp XP",
                        style = PixelTypography.displaySmall,
                        color = PixelGreen
                    )
                }
            }
        }

        // Level XP Bar
        val perfectDays = state.profile?.perfectDaysTowardNextLevel ?: 0
        val daysRequired = state.difficulty?.daysRequiredPerLevel ?: 7
        val userLevel = state.profile?.level ?: 1
        PixelXpBar(
            currentProgress = perfectDays,
            maxProgress = daysRequired,
            level = userLevel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
