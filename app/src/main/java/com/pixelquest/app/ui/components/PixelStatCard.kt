package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelStatCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    accentColor: Color = PixelGold,
    variant: PixelPanelVariant = PixelPanelVariant.BEIGE
) {
    PixelCard(
        variant = variant,
        contentPadding = 12.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = PixelTypography.displaySmall,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = PixelTypography.labelSmall,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = PixelTypography.titleMedium,
                    color = PixelTextWhite
                )
            }
        }
    }
}
