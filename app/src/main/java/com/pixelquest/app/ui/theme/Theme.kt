package com.pixelquest.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val PixelDarkColorScheme = darkColorScheme(
    primary = PixelGold,
    onPrimary = PixelBlack,
    primaryContainer = PixelGoldDark,
    onPrimaryContainer = PixelTextWhite,
    secondary = PixelCyan,
    onSecondary = PixelBlack,
    tertiary = PixelGreen,
    onTertiary = PixelBlack,
    background = PixelBackgroundDark,
    onBackground = PixelTextWhite,
    surface = PixelSurfaceDark,
    onSurface = PixelTextWhite,
    surfaceVariant = PixelSurfaceBorder,
    onSurfaceVariant = PixelTextMuted,
    error = PixelRed,
    onError = PixelTextWhite
)

/**
 * PixelQuest Theme wrapper supporting multiple theme modes:
 * - [ThemeMode.Pixel]: Classic retro dark arcade theme.
 * - [ThemeMode.Light]: Crisp light theme (full design Day 17).
 * - [ThemeMode.Comic]: Dynamic comic-book pop art (full design Days 20-23).
 */
@Composable
fun PixelQuestTheme(
    themeMode: ThemeMode = ThemeMode.Pixel,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColorScheme: AppColorScheme = when (themeMode) {
        ThemeMode.Pixel -> DefaultPixelColorScheme
        ThemeMode.Light -> DefaultLightColorScheme
        ThemeMode.Comic -> DefaultComicColorScheme
    }
    val materialColorScheme = appColorScheme.toMaterialColorScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.statusBarColor = appColorScheme.background.toArgb()
            activity?.window?.navigationBarColor = appColorScheme.background.toArgb()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppColorScheme provides appColorScheme,
        LocalAppThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = PixelTypography,
            content = content
        )
    }
}
