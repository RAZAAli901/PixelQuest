package com.pixelquest.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isSoundEnabled: Flow<Boolean>
    val isCrtEnabled: Flow<Boolean>
    val onboardingComplete: Flow<Boolean>
    val isNotificationsEnabled: Flow<Boolean>
    val isNotificationSoundEnabled: Flow<Boolean>
    val isNotificationVibrationEnabled: Flow<Boolean>

    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setCrtEnabled(enabled: Boolean)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationSoundEnabled(enabled: Boolean)
    suspend fun setNotificationVibrationEnabled(enabled: Boolean)
}
