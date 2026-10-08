package com.pixelquest.app.ui.haptics

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * PixelQuest Haptic Map defining consistent feedback patterns across the app.
 * Light tap: standard pixel button press
 * Medium confirm: dialog confirmation, deletion actions
 * Success moment: quest quick-complete, level-up celebration
 */
object PixelHaptics {
    /** Settings → HAPTICS, kept up to date by FeedbackSettingsSync. Every pattern respects it. */
    var isHapticsEnabledGlobal: Boolean = true

    // [enabled] lets a caller switch a pattern off (e.g. in Simple Mode); it can't switch on what the
    // player turned off. Only the light tap used to check the setting, so completing or skipping a
    // quest and confirm dialogs still vibrated with HAPTICS: OFF.
    private fun allowed(haptic: HapticFeedback?, enabled: Boolean) = enabled && isHapticsEnabledGlobal && haptic != null

    fun performLightTap(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!allowed(haptic, enabled)) return
        haptic!!.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performMediumConfirm(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!allowed(haptic, enabled)) return
        haptic!!.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performSuccessPattern(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!allowed(haptic, enabled)) return
        haptic!!.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performWarning(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!allowed(haptic, enabled)) return
        haptic!!.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
