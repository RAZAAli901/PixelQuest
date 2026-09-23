package com.pixelquest.app.ui.theme

/**
 * Step 1 (Day 21): Architectural dispatch strategy for UI components.
 *
 * Core components (PixelButton, PixelCard, PixelDialog, form controls) inspect
 * [PixelTheme.mode] / [LocalThemeMode.current] internally and branch to their corresponding
 * style renderer (Pixel, Light, or Comic).
 *
 * This design avoids codebase-wide renames or creating parallel Comic* component trees,
 * ensuring all existing screen call sites seamlessly adapt when theme switches occur.
 */
enum class ComponentThemeFamily {
    PIXEL,
    LIGHT,
    COMIC;

    companion object {
        fun fromThemeMode(mode: ThemeMode): ComponentThemeFamily = when (mode) {
            ThemeMode.Pixel, ThemeMode.System -> PIXEL
            ThemeMode.Light -> LIGHT
            ThemeMode.Comic -> COMIC
        }
    }
}
