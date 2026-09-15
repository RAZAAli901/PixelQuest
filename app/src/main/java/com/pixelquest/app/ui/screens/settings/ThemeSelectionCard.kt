package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelSurfaceBorder
import com.pixelquest.app.ui.theme.PixelTextMuted
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Theme selection section for Settings screen.
 * Presents Pixel, Light, and Comic theme options.
 * Comic is labeled "COMING SOON" until Days 20-23 design completion.
 */
@Composable
fun ThemeSelectionCard(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    PixelCard(
        variant = PixelPanelVariant.BLUE,
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "🎨 THEME SELECTION",
                style = PixelTypography.titleMedium,
                color = PixelGold
            )

            // Pixel Mode Option
            ThemeOptionRow(
                title = "🕹️ PIXEL (RETRO DARK)",
                subtitle = "Original 8-bit arcade aesthetic with CRT support",
                isSelected = currentTheme == ThemeMode.Pixel,
                isComingSoon = false,
                onClick = { onThemeSelected(ThemeMode.Pixel) }
            )

            // Light Mode Option
            ThemeOptionRow(
                title = "☀️ LIGHT (DAY MODE)",
                subtitle = "Crisp, modern productivity theme (Day 17 preview)",
                isSelected = currentTheme == ThemeMode.Light,
                isComingSoon = false,
                onClick = { onThemeSelected(ThemeMode.Light) }
            )

            // Comic Mode Option (Marked Coming Soon)
            ThemeOptionRow(
                title = "💥 COMIC (POP-ART)",
                subtitle = "Bold halftone pop-art borders (Coming Soon — Day 23)",
                isSelected = currentTheme == ThemeMode.Comic,
                isComingSoon = true,
                onClick = { /* Disabled / Coming soon */ }
            )
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isComingSoon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) PixelGold else PixelSurfaceBorder
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor)
            .clickable(enabled = !isComingSoon, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = PixelTypography.bodyMedium,
                    color = if (isComingSoon) PixelTextMuted else PixelTextWhite
                )
                if (isComingSoon) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[COMING SOON]",
                        style = PixelTypography.labelSmall.copy(fontSize = 9.sp),
                        color = PixelGold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = PixelTypography.bodySmall,
                color = PixelTextMuted
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isSelected) "● ACTIVE" else "○",
            style = PixelTypography.labelSmall,
            color = if (isSelected) PixelGold else PixelTextMuted
        )
    }
}
