package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Formal color scheme structure for the default 8-bit retro Pixel theme.
 * Preserves the exact palette tokens defined in Day 1 Color.kt.
 */
data class PixelColorScheme(
    val primary: Color = PixelGold,
    val onPrimary: Color = PixelBlack,
    val primaryContainer: Color = PixelGoldDark,
    val onPrimaryContainer: Color = PixelTextWhite,
    val secondary: Color = PixelCyan,
    val onSecondary: Color = PixelBlack,
    val secondaryContainer: Color = PixelCyanDark,
    val tertiary: Color = PixelGreen,
    val onTertiary: Color = PixelBlack,
    val tertiaryContainer: Color = PixelGreenDark,
    val background: Color = PixelBackgroundDark,
    val onBackground: Color = PixelTextWhite,
    val surface: Color = PixelSurfaceDark,
    val onSurface: Color = PixelTextWhite,
    val surfaceVariant: Color = PixelSurfaceBorder,
    val onSurfaceVariant: Color = PixelTextMuted,
    val error: Color = PixelRed,
    val onError: Color = PixelTextWhite,
    val accentPurple: Color = PixelPurple,
    val isDark: Boolean = true
) {
    fun toMaterialColorScheme(): ColorScheme = darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        tertiary = tertiary,
        onTertiary = onTertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError
    )
}

val DefaultPixelColorScheme = PixelColorScheme()
