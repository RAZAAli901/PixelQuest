package com.pixelquest.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isSoundEnabled: Flow<Boolean>
    val isCrtEnabled: Flow<Boolean>
    val isHapticsEnabled: Flow<Boolean>
    val isReduceMotionEnabled: Flow<Boolean>
    val onboardingComplete: Flow<Boolean>
    val isNotificationsEnabled: Flow<Boolean>
    val isNotificationSoundEnabled: Flow<Boolean>
    val isNotificationVibrationEnabled: Flow<Boolean>
    val themeMode: Flow<com.pixelquest.app.ui.theme.ThemeMode>
        get() = kotlinx.coroutines.flow.flowOf(com.pixelquest.app.ui.theme.ThemeMode.Pixel)

    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setCrtEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setReduceMotionEnabled(enabled: Boolean)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationSoundEnabled(enabled: Boolean)
    suspend fun setNotificationVibrationEnabled(enabled: Boolean)
    suspend fun setThemeMode(mode: com.pixelquest.app.ui.theme.ThemeMode) {}
}
