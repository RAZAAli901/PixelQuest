package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Placeholder LightColorScheme stub for ThemeMode.Light.
 * Architectural scaffolding for Day 17's full clean Light theme design.
 */
data class LightColorScheme(
    override val themeMode: ThemeMode = ThemeMode.Light,
    override val primary: Color = Color(0xFFD4A017),
    override val onPrimary: Color = Color(0xFF000000),
    override val primaryContainer: Color = Color(0xFFFFF4D0),
    override val onPrimaryContainer: Color = Color(0xFF3E2D00),
    override val secondary: Color = Color(0xFF00838F),
    override val onSecondary: Color = Color(0xFFFFFFFF),
    override val secondaryContainer: Color = Color(0xFFE0F7FA),
    override val tertiary: Color = Color(0xFF2E7D32),
    override val onTertiary: Color = Color(0xFFFFFFFF),
    override val tertiaryContainer: Color = Color(0xFFE8F5E9),
    override val background: Color = Color(0xFFF6F8FA),
    override val onBackground: Color = Color(0xFF24292F),
    override val surface: Color = Color(0xFFFFFFFF),
    override val onSurface: Color = Color(0xFF24292F),
    override val surfaceVariant: Color = Color(0xFFE1E4E8),
    override val onSurfaceVariant: Color = Color(0xFF57606A),
    override val error: Color = Color(0xFFD32F2F),
    override val onError: Color = Color(0xFFFFFFFF),
    override val accentPurple: Color = Color(0xFF7B1FA2),
    override val isDark: Boolean = false
) : AppColorScheme {
    override fun toMaterialColorScheme(): ColorScheme = lightColorScheme(
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

val DefaultLightColorScheme = LightColorScheme()
