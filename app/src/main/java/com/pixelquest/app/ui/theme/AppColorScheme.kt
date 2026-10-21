package com.pixelquest.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The colours the screens from before UI v2 ask for by role. There is one scheme, [PqAppColors],
 * which answers with v2's colours for the active skin (docs/ui-v2.md); new code uses [PqColors].
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
    val gold: Color
    val pixelBorder: Color
    val isDark: Boolean

    /** The primary colour for text drawn on [background] or [surface]: a darker ink that passes contrast. */
    val primaryText: Color get() = primary

    fun toMaterialColorScheme(): ColorScheme
}

/** The one [AppColorScheme]: each role is a v2 colour, read from the active skin when it is asked for. */
object PqAppColors : AppColorScheme {
    override val themeMode: ThemeMode get() = ThemeMode.Comic
    override val primary: Color get() = PqColors.Red
    override val onPrimary: Color get() = PqColors.OnRed
    override val primaryContainer: Color get() = PqColors.Amber
    override val onPrimaryContainer: Color get() = PqColors.Ink
    override val secondary: Color get() = PqColors.Sky
    override val onSecondary: Color get() = PqColors.Ink
    override val secondaryContainer: Color get() = PqColors.Sky
    override val tertiary: Color get() = PqColors.Lilac
    override val onTertiary: Color get() = PqColors.Ink
    override val tertiaryContainer: Color get() = PqColors.Lilac
    override val background: Color get() = PqColors.Ground
    override val onBackground: Color get() = PqColors.Ink
    override val surface: Color get() = PqColors.Paper
    override val onSurface: Color get() = PqColors.Ink
    override val surfaceVariant: Color get() = PqColors.Sunken
    override val onSurfaceVariant: Color get() = PqColors.Muted
    override val error: Color get() = PqColors.Error
    override val onError: Color get() = PqColors.Paper
    override val accentPurple: Color get() = PqColors.Lilac
    override val gold: Color get() = PqColors.Gold
    override val pixelBorder: Color get() = PqColors.Ink
    override val isDark: Boolean get() = false
    override val primaryText: Color get() = PqColors.RedText

    override fun toMaterialColorScheme(): ColorScheme = pqMaterialColors()
}

/**
 * CompositionLocal providing access to the current active [AppColorScheme].
 */
val LocalAppColorScheme = androidx.compose.runtime.staticCompositionLocalOf<AppColorScheme> {
    PqAppColors
}

/**
 * The route the screens from before UI v2 take: always [ThemeMode.Comic]'s ink and shadows, whose
 * tokens ([ComicTokens], [ComicShapeTokens]) follow the active skin.
 */
val LocalAppThemeMode = androidx.compose.runtime.staticCompositionLocalOf<ThemeMode> {
    ThemeMode.Comic
}

/**
 * Alias for [LocalAppThemeMode] providing direct access to the current [ThemeMode].
 */
val LocalThemeMode = LocalAppThemeMode

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
