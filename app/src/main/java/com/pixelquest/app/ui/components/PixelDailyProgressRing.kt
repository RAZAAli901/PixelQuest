package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelDailyProgressRing(
    progress: Float,
    targetThreshold: Float,
    modifier: Modifier = Modifier
) {
    val pctInt = (progress * 100).toInt()
    val targetPctInt = (targetThreshold * 100).toInt()
    val isGoalMet = progress >= targetThreshold

    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isGoalMet) "⭐ PERFECT DAY!" else "🎯 QUEST PROGRESS",
                    style = PixelTypography.titleMedium,
                    color = if (isGoalMet) PixelGold else PixelTextWhite,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$pctInt% / $targetPctInt%",
                    style = PixelTypography.labelMedium,
                    color = if (isGoalMet) PixelGreen else PixelCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            PixelProgressBar(
                progress = progress,
                height = 18.dp
            )
        }
    }
}
