package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelPerfectDayBanner(
    modifier: Modifier = Modifier
) {
    PixelCard(
        variant = PixelPanelVariant.YELLOW,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🎉 PERFECT DAY ACHIEVED! Streak protected for today!",
                style = PixelTypography.bodyMedium,
                color = PixelGold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
