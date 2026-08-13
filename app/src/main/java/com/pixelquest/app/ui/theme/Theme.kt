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

@Composable
fun PixelQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = PixelDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.statusBarColor = colorScheme.background.toArgb()
            activity?.window?.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelTypography,
        content = content
    )
}
