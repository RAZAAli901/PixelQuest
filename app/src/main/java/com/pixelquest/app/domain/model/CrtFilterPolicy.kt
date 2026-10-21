package com.pixelquest.app.domain.model

import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Whether the CRT scanline filter is on: only in the Pixel skin, only if the player turned it on,
 * and never in Simple Mode.
 */
object CrtFilterPolicy {
    fun shouldApplyCrt(
        isCrtSettingEnabled: Boolean,
        effectiveThemeMode: ThemeMode,
        isSimpleModeEnabled: Boolean
    ): Boolean = isCrtSettingEnabled && effectiveThemeMode == ThemeMode.Pixel && !isSimpleModeEnabled
}
