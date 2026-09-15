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

/**
 * CompositionLocal providing access to the current active [AppColorScheme].
 */
val LocalAppColorScheme = androidx.compose.runtime.staticCompositionLocalOf<AppColorScheme> {
    DefaultPixelColorScheme
}

/**
 * CompositionLocal providing access to the current active [ThemeMode].
 */
val LocalAppThemeMode = androidx.compose.runtime.staticCompositionLocalOf<ThemeMode> {
    ThemeMode.Pixel
}

/**
 * Convenient accessor for theme tokens in composables.
 */
object PixelTheme {
    val colors: AppColorScheme
        @androidx.compose.runtime.Composable
        get() = LocalAppColorScheme.current

    val mode: ThemeMode
        @androidx.compose.runtime.Composable
        get() = LocalAppThemeMode.current
}
