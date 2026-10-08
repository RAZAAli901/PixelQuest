package com.pixelquest.app.audio

import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.haptics.PixelHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Applies Settings → SFX and HAPTICS app-wide for as long as [scope] lives (the application's).
 * SFX used to be saved and never reach SoundManager, so turning it off silenced nothing; haptics were
 * only applied inside MainActivity, so the full-screen prompt ignored them.
 */
object FeedbackSettingsSync {
    fun start(scope: CoroutineScope, settings: SettingsRepository, soundManager: SoundManager) {
        scope.launch { settings.isSoundEnabled.collect { soundManager.isSoundEnabled = it } }
        scope.launch { settings.isHapticsEnabled.collect { PixelHaptics.isHapticsEnabledGlobal = it } }
    }
}
