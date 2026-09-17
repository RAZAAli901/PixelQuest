package com.pixelquest.app.domain.model

import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Step 11: Policy determining CRT scanline filter application.
 * CRT filter is restricted to Pixel theme mode only, and is strictly forced OFF
 * when Simple Mode is active, preserving an understated, distraction-free environment.
 */
object CrtFilterPolicy {
    fun shouldApplyCrt(
        isCrtSettingEnabled: Boolean,
        effectiveThemeMode: ThemeMode,
        isSimpleModeEnabled: Boolean
    ): Boolean {
        return isCrtSettingEnabled && effectiveThemeMode == ThemeMode.Pixel && !isSimpleModeEnabled
    }
}
