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
fun FlavorTextBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode
    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors
    val textColor = if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        com.pixelquest.app.ui.theme.ComicTokens.SolidBlack
    } else {
        colors.secondary
    }

    PixelCard(
        variant = PixelPanelVariant.BEIGE,
        contentPadding = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "💬 $text",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
