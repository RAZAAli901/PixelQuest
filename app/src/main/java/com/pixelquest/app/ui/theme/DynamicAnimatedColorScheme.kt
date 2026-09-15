package com.pixelquest.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Dynamic AppColorScheme that holds interpolated color values during theme transitions.
 */
data class DynamicAnimatedColorScheme(
    override val themeMode: ThemeMode,
    override val primary: Color,
    override val onPrimary: Color,
    override val primaryContainer: Color,
    override val onPrimaryContainer: Color,
    override val secondary: Color,
    override val onSecondary: Color,
    override val secondaryContainer: Color,
    override val tertiary: Color,
    override val onTertiary: Color,
    override val tertiaryContainer: Color,
    override val background: Color,
    override val onBackground: Color,
    override val surface: Color,
    override val onSurface: Color,
    override val surfaceVariant: Color,
    override val onSurfaceVariant: Color,
    override val error: Color,
    override val onError: Color,
    override val accentPurple: Color,
    override val isDark: Boolean
) : AppColorScheme {
    override fun toMaterialColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
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
        } else {
            lightColorScheme(
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
    }
}

/**
 * Interpolates color scheme changes with a smooth cross-fade animation (300ms),
 * honoring user's reduce-motion accessibility preference.
 */
@Composable
fun rememberAnimatedAppColorScheme(
    targetScheme: AppColorScheme,
    isReduceMotion: Boolean = false
): AppColorScheme {
    if (isReduceMotion) return targetScheme

    val animationSpec = tween<Color>(durationMillis = 300)

    val primary by animateColorAsState(targetScheme.primary, animationSpec, label = "theme_primary")
    val onPrimary by animateColorAsState(targetScheme.onPrimary, animationSpec, label = "theme_onPrimary")
    val primaryContainer by animateColorAsState(targetScheme.primaryContainer, animationSpec, label = "theme_primaryContainer")
    val onPrimaryContainer by animateColorAsState(targetScheme.onPrimaryContainer, animationSpec, label = "theme_onPrimaryContainer")
    val secondary by animateColorAsState(targetScheme.secondary, animationSpec, label = "theme_secondary")
    val onSecondary by animateColorAsState(targetScheme.onSecondary, animationSpec, label = "theme_onSecondary")
    val secondaryContainer by animateColorAsState(targetScheme.secondaryContainer, animationSpec, label = "theme_secondaryContainer")
    val tertiary by animateColorAsState(targetScheme.tertiary, animationSpec, label = "theme_tertiary")
    val onTertiary by animateColorAsState(targetScheme.onTertiary, animationSpec, label = "theme_onTertiary")
    val tertiaryContainer by animateColorAsState(targetScheme.tertiaryContainer, animationSpec, label = "theme_tertiaryContainer")
    val background by animateColorAsState(targetScheme.background, animationSpec, label = "theme_background")
    val onBackground by animateColorAsState(targetScheme.onBackground, animationSpec, label = "theme_onBackground")
    val surface by animateColorAsState(targetScheme.surface, animationSpec, label = "theme_surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, animationSpec, label = "theme_onSurface")
    val surfaceVariant by animateColorAsState(targetScheme.surfaceVariant, animationSpec, label = "theme_surfaceVariant")
    val onSurfaceVariant by animateColorAsState(targetScheme.onSurfaceVariant, animationSpec, label = "theme_onSurfaceVariant")
    val error by animateColorAsState(targetScheme.error, animationSpec, label = "theme_error")
    val onError by animateColorAsState(targetScheme.onError, animationSpec, label = "theme_onError")
    val accentPurple by animateColorAsState(targetScheme.accentPurple, animationSpec, label = "theme_accentPurple")

    return remember(targetScheme.themeMode, primary, background, surface, surfaceVariant) {
        DynamicAnimatedColorScheme(
            themeMode = targetScheme.themeMode,
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError,
            accentPurple = accentPurple,
            isDark = targetScheme.isDark
        )
    }
}
