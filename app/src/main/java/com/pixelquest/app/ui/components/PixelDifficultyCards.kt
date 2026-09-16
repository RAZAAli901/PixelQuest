package com.pixelquest.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.pixelquest.app.domain.DifficultyMode
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelThemeAssetFilter

private fun getDifficultyIconRes(level: DifficultyLevel): Int = when (level) {
    DifficultyLevel.EASY -> R.drawable.ic_diff_easy
    DifficultyLevel.MEDIUM -> R.drawable.ic_diff_medium
    DifficultyLevel.HARD -> R.drawable.ic_diff_hard
    DifficultyLevel.HARDEST -> R.drawable.ic_diff_hardest
}

@Composable
fun PixelDifficultyCards(
    selectedLevel: DifficultyLevel,
    onLevelSelected: (DifficultyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PixelTheme.colors
    val mode = PixelTheme.mode

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DifficultyLevel.values().forEach { level ->
            val isSelected = level == selectedLevel
            val thresholdPct = (DifficultyMode.getPerfectDayThreshold(level) * 100).toInt()
            val daysReq = DifficultyMode.getDaysRequiredPerLevel(level)

            PixelCard(
                variant = if (isSelected) PixelPanelVariant.BLUE else PixelPanelVariant.BORDER,
                contentPadding = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLevelSelected(level) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = getDifficultyIconRes(level)),
                        contentDescription = DifficultyMode.getDisplayName(level),
                        colorFilter = PixelThemeAssetFilter.forTheme(
                            mode,
                            if (isSelected) colors.primary else colors.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DifficultyMode.getDisplayName(level).uppercase(),
                            style = PixelTypography.titleMedium,
                            color = if (isSelected) colors.primary else colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Perfect Day: $thresholdPct% completed",
                            style = PixelTypography.bodySmall,
                            color = if (isSelected) colors.secondary else colors.onSurfaceVariant
                        )
                        Text(
                            text = "Days per Level: $daysReq days",
                            style = PixelTypography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Text(
                            text = "ACTIVE",
                            style = PixelTypography.labelMedium,
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}
