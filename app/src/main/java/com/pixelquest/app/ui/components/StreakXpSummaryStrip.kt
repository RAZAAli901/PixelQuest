package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun StreakXpSummaryStrip(
    currentStreak: Int,
    totalXp: Int,
    level: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelCard(
        variant = PixelPanelVariant.BLUE,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥 ",
                    style = PixelTypography.titleMedium
                )
                Text(
                    text = "$currentStreak DAYS",
                    style = PixelTypography.titleMedium,
                    color = PixelGold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✨ ",
                    style = PixelTypography.titleMedium
                )
                Text(
                    text = "$totalXp XP",
                    style = PixelTypography.titleMedium,
                    color = PixelGreen
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⭐ ",
                    style = PixelTypography.titleMedium
                )
                Text(
                    text = "LVL $level",
                    style = PixelTypography.titleMedium,
                    color = PixelCyan
                )
            }
        }
    }
}
