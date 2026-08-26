package com.pixelquest.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pixelquest.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("pixelquest_settings", Context.MODE_PRIVATE)
    }

    override val isSoundEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SOUND_ENABLED) {
                trySend(prefs.getBoolean(KEY_SOUND_ENABLED, true))
            }
        }
        trySend(prefs.getBoolean(KEY_SOUND_ENABLED, true))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val isCrtEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CRT_ENABLED) {
                trySend(prefs.getBoolean(KEY_CRT_ENABLED, false))
            }
        }
        trySend(prefs.getBoolean(KEY_CRT_ENABLED, false))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val onboardingComplete: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ONBOARDING_COMPLETE) {
                trySend(prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
            }
        }
        trySend(prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val isNotificationsEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NOTIFICATIONS_ENABLED) {
                trySend(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
            }
        }
        trySend(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val isNotificationSoundEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NOTIFICATION_SOUND) {
                trySend(prefs.getBoolean(KEY_NOTIFICATION_SOUND, true))
            }
        }
        trySend(prefs.getBoolean(KEY_NOTIFICATION_SOUND, true))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val isNotificationVibrationEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NOTIFICATION_VIBRATION) {
                trySend(prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true))
            }
        }
        trySend(prefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    override suspend fun setCrtEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CRT_ENABLED, enabled).apply()
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply()
    }

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, enabled).apply()
    }

    companion object {
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_CRT_ENABLED = "key_crt_enabled"
        private const val KEY_ONBOARDING_COMPLETE = "key_onboarding_complete"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
        private const val KEY_NOTIFICATION_SOUND = "key_notification_sound"
        private const val KEY_NOTIFICATION_VIBRATION = "key_notification_vibration"
    }
}
