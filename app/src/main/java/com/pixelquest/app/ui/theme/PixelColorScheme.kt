package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Formal color scheme structure for the default 8-bit retro Pixel theme.
 * Preserves the exact palette tokens defined in Day 1 Color.kt.
 */
data class PixelColorScheme(
    override val themeMode: ThemeMode = ThemeMode.Pixel,
    override val primary: Color = PixelGold,
    override val onPrimary: Color = PixelBlack,
    override val primaryContainer: Color = PixelGoldDark,
    override val onPrimaryContainer: Color = PixelTextWhite,
    override val secondary: Color = PixelCyan,
    override val onSecondary: Color = PixelBlack,
    override val secondaryContainer: Color = PixelCyanDark,
    override val tertiary: Color = PixelGreen,
    override val onTertiary: Color = PixelBlack,
    override val tertiaryContainer: Color = PixelGreenDark,
    override val background: Color = PixelBackgroundDark,
    override val onBackground: Color = PixelTextWhite,
    override val surface: Color = PixelSurfaceDark,
    override val onSurface: Color = PixelTextWhite,
    override val surfaceVariant: Color = PixelSurfaceBorder,
    override val onSurfaceVariant: Color = PixelTextMuted,
    override val error: Color = PixelRed,
    override val onError: Color = PixelTextWhite,
    override val accentPurple: Color = PixelPurple,
    override val isDark: Boolean = true
) : AppColorScheme {
    override fun toMaterialColorScheme(): ColorScheme = darkColorScheme(
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
