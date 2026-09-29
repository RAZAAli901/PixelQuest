package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelTheme

@Composable
fun StreakXpSummaryStrip(
    currentStreak: Int,
    totalXp: Int,
    level: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
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
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$currentStreak DAYS",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.gold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✨ ",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$totalXp XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.tertiary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⭐ ",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "LVL $level",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.secondary
                )
            }
        }
    }
}
