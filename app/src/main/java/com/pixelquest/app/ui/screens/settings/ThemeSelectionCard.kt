package com.pixelquest.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.DefaultComicColorScheme
import com.pixelquest.app.ui.theme.DefaultLightColorScheme
import com.pixelquest.app.ui.theme.DefaultPixelColorScheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Theme selection section for Settings screen.
 * Presents Pixel, Light, and Comic theme options with color swatches.
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
                color = com.pixelquest.app.ui.theme.PixelTheme.colors.primary
            )

            // Follow System Option
            ThemeOptionRow(
                title = "📱 FOLLOW SYSTEM",
                subtitle = "Automatically match device light/dark schedule",
                isSelected = currentTheme == ThemeMode.System,
                isComingSoon = false,
                previewColors = listOf(
                    DefaultPixelColorScheme.background,
                    DefaultLightColorScheme.background,
                    DefaultPixelColorScheme.primary,
                    DefaultLightColorScheme.primary
                ),
                onClick = { onThemeSelected(ThemeMode.System) }
            )

            // Pixel Mode Option
            ThemeOptionRow(
                title = "🕹️ PIXEL (RETRO DARK)",
                subtitle = "Original 8-bit arcade aesthetic with CRT support",
                isSelected = currentTheme == ThemeMode.Pixel,
                isComingSoon = false,
                previewColors = listOf(
                    DefaultPixelColorScheme.background,
                    DefaultPixelColorScheme.primary,
                    DefaultPixelColorScheme.secondary,
                    DefaultPixelColorScheme.tertiary
                ),
                onClick = { onThemeSelected(ThemeMode.Pixel) }
            )

            // Light Mode Option
            ThemeOptionRow(
                title = "☀️ LIGHT (DAY MODE)",
                subtitle = "Crisp, modern productivity theme (Day 17 preview)",
                isSelected = currentTheme == ThemeMode.Light,
                isComingSoon = false,
                previewColors = listOf(
                    DefaultLightColorScheme.background,
                    DefaultLightColorScheme.primary,
                    DefaultLightColorScheme.secondary,
                    DefaultLightColorScheme.tertiary
                ),
                onClick = { onThemeSelected(ThemeMode.Light) }
            )

            // Comic Mode Option (Marked Coming Soon)
            ThemeOptionRow(
                title = "💥 COMIC (POP-ART)",
                subtitle = "Bold pop-art comic borders (Coming Soon — Day 23)",
                isSelected = currentTheme == ThemeMode.Comic,
                isComingSoon = !ThemeMode.Comic.isAvailable,
                previewColors = listOf(
                    DefaultComicColorScheme.primary,
                    DefaultComicColorScheme.burntOrange,
                    DefaultComicColorScheme.skyBlue,
                    DefaultComicColorScheme.lavender,
                    DefaultComicColorScheme.comicBorder
                ),
                onClick = { /* Disabled / Gated until Day 23 */ }
            )
        }
    }
}

/**
 * Small 4-tile palette swatch displaying the primary colors of the theme.
 */
@Composable
fun ThemePreviewSwatch(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(1.dp, com.pixelquest.app.ui.theme.PixelTheme.colors.pixelBorder)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.4f))
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
    previewColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = com.pixelquest.app.ui.theme.PixelTheme.colors
    val borderColor = if (isSelected) themeColors.primary else themeColors.pixelBorder
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
                    color = if (isComingSoon) themeColors.onSurfaceVariant else themeColors.onSurface
                )
                if (isComingSoon) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[COMING SOON]",
                        style = PixelTypography.labelSmall.copy(fontSize = 9.sp),
                        color = themeColors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = PixelTypography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            ThemePreviewSwatch(colors = previewColors)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isSelected) "● ACTIVE" else "○",
            style = PixelTypography.labelSmall,
            color = if (isSelected) themeColors.primary else themeColors.onSurfaceVariant
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF12121E)
@Composable
fun ThemeSelectionCardPreview() {
    com.pixelquest.app.ui.theme.PixelQuestTheme {
        ThemeSelectionCard(
            currentTheme = ThemeMode.Pixel,
            onThemeSelected = {}
        )
    }
}
