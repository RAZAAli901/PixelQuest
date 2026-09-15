package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Placeholder ComicColorScheme stub for ThemeMode.Comic.
 * Architectural scaffolding for Days 20-23's full comic-book alternate theme design.
 */
data class ComicColorScheme(
    override val themeMode: ThemeMode = ThemeMode.Comic,
    override val primary: Color = Color(0xFFFFD600), // Bold comic yellow
    override val onPrimary: Color = Color(0xFF000000),
    override val primaryContainer: Color = Color(0xFFFF6D00), // Comic punch orange
    override val onPrimaryContainer: Color = Color(0xFF000000),
    override val secondary: Color = Color(0xFF00B0FF), // Comic dynamic cyan
    override val onSecondary: Color = Color(0xFF000000),
    override val secondaryContainer: Color = Color(0xFF0091EA),
    override val tertiary: Color = Color(0xFFFF4081), // Comic action pink
    override val onTertiary: Color = Color(0xFFFFFFFF),
    override val tertiaryContainer: Color = Color(0xFFC51162),
    override val background: Color = Color(0xFF1A1A2E), // Deep comic night blue
    override val onBackground: Color = Color(0xFFFFFFFF),
    override val surface: Color = Color(0xFF16213E), // Comic panel blue
    override val onSurface: Color = Color(0xFFFFFFFF),
    override val surfaceVariant: Color = Color(0xFF0F3460),
    override val onSurfaceVariant: Color = Color(0xFFB0BEC5),
    override val error: Color = Color(0xFFFF1744),
    override val onError: Color = Color(0xFFFFFFFF),
    override val accentPurple: Color = Color(0xFF7C4DFF),
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

val DefaultComicColorScheme = ComicColorScheme()
