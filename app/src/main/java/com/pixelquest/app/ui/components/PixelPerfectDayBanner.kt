package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PixelPerfectDayBanner(
    modifier: Modifier = Modifier,
    isSimpleMode: Boolean = false
) {
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode
    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors
    val textColor = if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
    } else {
        if (isSimpleMode) colors.primary else colors.gold
    }

    PixelCard(
        variant = if (isSimpleMode) PixelPanelVariant.BORDER else PixelPanelVariant.BEIGE,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isSimpleMode) "✓ All tasks done for today" else "🎉 PERFECT DAY ACHIEVED! Streak protected for today!",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
