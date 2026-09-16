package com.pixelquest.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView


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
    isReduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveMode = themeMode.resolveEffective(darkTheme)
    val rawColorScheme: AppColorScheme = when (effectiveMode) {
        ThemeMode.Pixel, ThemeMode.System -> DefaultPixelColorScheme
        ThemeMode.Light -> DefaultLightColorScheme
        ThemeMode.Comic -> DefaultComicColorScheme
    }
    val appColorScheme = rememberAnimatedAppColorScheme(
        targetScheme = rawColorScheme,
        isReduceMotion = isReduceMotion
    )
    val materialColorScheme = appColorScheme.toMaterialColorScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = appColorScheme.background.toArgb()
                window.navigationBarColor = appColorScheme.background.toArgb()
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                val isLight = effectiveMode == ThemeMode.Light
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppColorScheme provides appColorScheme,
        LocalAppThemeMode provides effectiveMode
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = PixelTypography,
            content = content
        )
    }
}
