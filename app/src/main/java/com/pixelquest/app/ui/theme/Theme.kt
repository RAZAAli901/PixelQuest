package com.pixelquest.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Settings > REDUCE MOTION. Looping animations (pulses, spinners, the level-up bounce) hold still
 * while it is on; the theme's colour change is instant.
 */
val LocalReduceMotion = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * The app's theme: UI v2's tokens in the active skin (docs/ui-v2.md), and Material's colours and type
 * from them for the screens from before UI v2. The app sets the skin itself, before setContent and
 * as Settings > Look changes, and leaves [themeMode] null. A [themeMode] given (a test, a preview)
 * wears that skin for as long as this theme is shown, and gives the one before it back after.
 */
@Composable
fun PixelQuestTheme(
    themeMode: ThemeMode? = null,
    isReduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    if (themeMode != null) {
        val before = remember { PqSkinState.skin }
        if (PqSkinState.skin != themeMode.skin) PqSkinState.skin = themeMode.skin
        DisposableEffect(Unit) { onDispose { PqSkinState.skin = before } }
    }
    val skin = PqSkinState.skin
    val colors = remember(skin) { pqMaterialColors() }
    val typography = remember(skin) { pqTypography() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColorScheme provides PqAppColors,
        LocalReduceMotion provides isReduceMotion
    ) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
