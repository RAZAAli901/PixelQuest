package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Comic Book design tokens extracted from Nitnode reference aesthetics:
 * - Coral-red primary CTA accent (#FF5A4E)
 * - Three container variants: burnt orange (#F0A868), sky blue (#8ECAE6), lavender (#B8A4D4)
 * - High-contrast solid black border and flat offset drop-shadow (#000000)
 * - Warm light neutral newsprint background (#FAF8F5) and crisp white surface (#FFFFFF)
 */
object ComicTokens {
    val CoralRed = Color(0xFFFF5A4E)
    val BurntOrange = Color(0xFFF0A868)
    val SkyBlue = Color(0xFF8ECAE6)
    val Lavender = Color(0xFFB8A4D4)
    val SolidBlack = Color(0xFF000000)
    val PaperBackground = Color(0xFFFAF8F5)
    val PanelSurface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF4EFE6)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF4A4A4A)
    val GoldAccent = Color(0xFFFFB703)
    val ErrorRed = Color(0xFFD32F2F)
}

/**
 * ComicColorScheme implementing the AppColorScheme contract with the finalized comic palette.
 */
data class ComicColorScheme(
    override val themeMode: ThemeMode = ThemeMode.Comic,
    override val primary: Color = ComicTokens.CoralRed,
    override val onPrimary: Color = ComicTokens.SolidBlack,
    override val primaryContainer: Color = ComicTokens.BurntOrange,
    override val onPrimaryContainer: Color = ComicTokens.SolidBlack,
    override val secondary: Color = ComicTokens.SkyBlue,
    override val onSecondary: Color = ComicTokens.SolidBlack,
    override val secondaryContainer: Color = ComicTokens.SkyBlue,
    override val tertiary: Color = ComicTokens.Lavender,
    override val onTertiary: Color = ComicTokens.SolidBlack,
    override val tertiaryContainer: Color = ComicTokens.Lavender,
    override val background: Color = ComicTokens.PaperBackground,
    override val onBackground: Color = ComicTokens.TextPrimary,
    override val surface: Color = ComicTokens.PanelSurface,
    override val onSurface: Color = ComicTokens.TextPrimary,
    override val surfaceVariant: Color = ComicTokens.SurfaceVariant,
    override val onSurfaceVariant: Color = ComicTokens.TextSecondary,
    override val error: Color = ComicTokens.ErrorRed,
    override val onError: Color = Color(0xFFFFFFFF),
    override val accentPurple: Color = ComicTokens.Lavender,
    override val gold: Color = ComicTokens.GoldAccent,
    override val pixelBorder: Color = ComicTokens.SolidBlack,
    override val isDark: Boolean = false,
    // Direct accessors for Comic container variants
    val burntOrange: Color = ComicTokens.BurntOrange,
    val skyBlue: Color = ComicTokens.SkyBlue,
    val lavender: Color = ComicTokens.Lavender,
    val comicBorder: Color = ComicTokens.SolidBlack,
    val comicShadow: Color = ComicTokens.SolidBlack
) : AppColorScheme {
    override fun toMaterialColorScheme(): ColorScheme = lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
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

val DefaultComicColorScheme = ComicColorScheme()
