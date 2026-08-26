package com.pixelquest.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isSoundEnabled: Flow<Boolean>
    val isCrtEnabled: Flow<Boolean>
    val onboardingComplete: Flow<Boolean>
    val isNotificationsEnabled: Flow<Boolean>

    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setCrtEnabled(enabled: Boolean)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
