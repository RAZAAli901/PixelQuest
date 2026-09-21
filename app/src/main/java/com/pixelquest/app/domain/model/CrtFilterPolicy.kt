package com.pixelquest.app.domain.model

import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Policy determining CRT scanline filter application.
 * CRT filter is strictly restricted to retro dark arcade Pixel mode only:
 * - [ThemeMode.Pixel]: Allowed if user enabled CRT setting and Simple Mode is inactive.
 * - [ThemeMode.Light]: Excluded by policy (crisp daylight mode).
 * - [ThemeMode.Comic]: Excluded by policy (Day 20 Step 22: pop-art paper mode has its own visual language).
 * - Simple Mode: Strictly forced OFF across all themes.
 */
object CrtFilterPolicy {
    fun shouldApplyCrt(
        isCrtSettingEnabled: Boolean,
        effectiveThemeMode: ThemeMode,
        isSimpleModeEnabled: Boolean
    ): Boolean {
        // Explicitly reject Comic and Light modes
        if (effectiveThemeMode == ThemeMode.Comic || effectiveThemeMode == ThemeMode.Light) {
            return false
        }
        return isCrtSettingEnabled && effectiveThemeMode == ThemeMode.Pixel && !isSimpleModeEnabled
    }
}
