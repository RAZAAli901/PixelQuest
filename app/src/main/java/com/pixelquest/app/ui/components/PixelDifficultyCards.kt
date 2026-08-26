package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelDifficultyCards(
    selectedLevel: DifficultyLevel,
    onLevelSelected: (DifficultyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DifficultyLevel.values().forEach { level ->
            val isSelected = level == selectedLevel
            val thresholdPct = (DifficultyMode.getPerfectDayThreshold(level) * 100).toInt()
            val daysReq = DifficultyMode.getDaysRequiredPerLevel(level)

            PixelCard(
                variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BEIGE,
                contentPadding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLevelSelected(level) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DifficultyMode.getDisplayName(level).uppercase(),
                            style = PixelTypography.titleMedium,
                            color = if (isSelected) PixelGold else PixelGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Perfect Day: $thresholdPct% completed",
                            style = PixelTypography.bodySmall,
                            color = PixelCyan
                        )
                        Text(
                            text = "Days per Level: $daysReq days",
                            style = PixelTypography.labelSmall,
                            color = PixelTextWhite
                        )
                    }
                    if (isSelected) {
                        Text(
                            text = "ACTIVE",
                            style = PixelTypography.labelMedium,
                            color = PixelGold
                        )
                    }
                }
            }
        }
    }
}
