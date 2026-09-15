package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit test for theme preference persistence and retrieval via [SettingsRepository].
 * Simulates persistent key-value storage across repository re-instantiations.
 */
class ThemePreferencePersistenceTest {

    // Persistent storage simulation (survives repository instances)
    private val persistentStore = mutableMapOf<String, String>()

    // Fake implementation simulating SettingsRepository backed by persistentStore
    private inner class TestableSettingsRepository : SettingsRepository {
        private val _themeFlow = MutableStateFlow(
            ThemeMode.fromId(persistentStore[KEY_THEME_MODE] ?: ThemeMode.Pixel.id)
        )

        override val themeMode: Flow<ThemeMode> = _themeFlow.asStateFlow()
        override val isSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isCrtEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val isHapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isReduceMotionEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val onboardingComplete: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = MutableStateFlow(true)

        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}

        override suspend fun setThemeMode(mode: ThemeMode) {
            persistentStore[KEY_THEME_MODE] = mode.id
            _themeFlow.value = mode
        }
    }

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        persistentStore.clear()
        repository = TestableSettingsRepository()
    }

    @Test
    fun defaultThemePreferenceIsPixel() = runBlocking {
        val initialMode = repository.themeMode.first()
        assertEquals(ThemeMode.Pixel, initialMode)
    }

    @Test
    fun settingThemeModeUpdatesFlowReactively() = runBlocking {
        repository.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.Comic)
        assertEquals(ThemeMode.Comic, repository.themeMode.first())
    }

    @Test
    fun themePreferencePersistsAcrossRepositoryRecreation() = runBlocking {
        // User selects Light mode
        repository.setThemeMode(ThemeMode.Light)

        // Simulate app restart / new repository instance reading from same backing store
        val reloadedRepository = TestableSettingsRepository()
        val restoredMode = reloadedRepository.themeMode.first()

        assertEquals(ThemeMode.Light, restoredMode)
    }

    @Test
    fun fallbackToPixelForUnrecognizedStorageKey() {
        persistentStore[KEY_THEME_MODE] = "unknown_theme_variant"
        val restoredMode = ThemeMode.fromId(persistentStore[KEY_THEME_MODE])
        assertEquals(ThemeMode.Pixel, restoredMode)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
    }
}
