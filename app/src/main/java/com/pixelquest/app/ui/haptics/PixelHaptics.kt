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
    fun performLightTap(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!enabled || haptic == null) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performMediumConfirm(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!enabled || haptic == null) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performSuccessPattern(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!enabled || haptic == null) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performWarning(haptic: HapticFeedback?, enabled: Boolean = true) {
        if (!enabled || haptic == null) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
