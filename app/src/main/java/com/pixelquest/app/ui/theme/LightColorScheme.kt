package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Finalized LightColorScheme for ThemeMode.Light.
 * Retro Arcade in Daylight: warm parchment canvas, high-contrast quest amber primary,
 * daylight cyan secondary, meadow HP green tertiary, and deep stone charcoal typography.
 */
data class LightColorScheme(
    override val themeMode: ThemeMode = ThemeMode.Light,
    override val primary: Color = Color(0xFFB45309), // Warm Quest Gold / Adventurer Amber
    override val onPrimary: Color = Color(0xFFFFFFFF),
    override val primaryContainer: Color = Color(0xFFFEF3C7),
    override val onPrimaryContainer: Color = Color(0xFF451A03),
    override val secondary: Color = Color(0xFF0284C7), // Daylight Sky / Retro Cyan
    override val onSecondary: Color = Color(0xFFFFFFFF),
    override val secondaryContainer: Color = Color(0xFFE0F2FE),
    override val tertiary: Color = Color(0xFF15803D), // Meadow Quest Green / HP Green
    override val onTertiary: Color = Color(0xFFFFFFFF),
    override val tertiaryContainer: Color = Color(0xFFDCFCE7),
    override val background: Color = Color(0xFFF8F6F0), // Warm retro ivory / cartridge parchment
    override val onBackground: Color = Color(0xFF1C1917), // Deep stone charcoal (WCAG AAA)
    override val surface: Color = Color(0xFFFFFFFF), // Crisp card white
    override val onSurface: Color = Color(0xFF1C1917),
    override val surfaceVariant: Color = Color(0xFFE6E1D6), // Warm retro divider / border tone
    override val onSurfaceVariant: Color = Color(0xFF57534E), // Muted warm graphite
    override val error: Color = Color(0xFFDC2626), // Trap / boss red
    override val onError: Color = Color(0xFFFFFFFF),
    override val accentPurple: Color = Color(0xFF7E22CE), // Mystic rune purple
    override val gold: Color = Color(0xFFA16207), // Deep Dungeon Gold (>5:1 on light bg)
    override val pixelBorder: Color = Color(0xFF292524), // 8-bit stone border
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
