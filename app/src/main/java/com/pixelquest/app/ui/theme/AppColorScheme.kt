package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Universal color contract implemented by all PixelQuest theme variants:
 * - [PixelColorScheme] (Retro Dark 8-bit Arcade)
 * - [LightColorScheme] (Clean Light Mode — Day 17)
 * - [ComicColorScheme] (Bold Comic Pop-Art — Days 20-23)
 */
interface AppColorScheme {
    val themeMode: ThemeMode
    val primary: Color
    val onPrimary: Color
    val primaryContainer: Color
    val onPrimaryContainer: Color
    val secondary: Color
    val onSecondary: Color
    val secondaryContainer: Color
    val tertiary: Color
    val onTertiary: Color
    val tertiaryContainer: Color
    val background: Color
    val onBackground: Color
    val surface: Color
    val onSurface: Color
    val surfaceVariant: Color
    val onSurfaceVariant: Color
    val error: Color
    val onError: Color
    val accentPurple: Color
    val isDark: Boolean

    fun toMaterialColorScheme(): ColorScheme
}
