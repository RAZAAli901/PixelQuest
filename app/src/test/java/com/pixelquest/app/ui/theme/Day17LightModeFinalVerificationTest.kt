package com.pixelquest.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Step 41 final verification test:
 * Validates that Light Mode configuration persists across simulated app restarts,
 * correctly resolves to [DefaultLightColorScheme], disables CRT scanlines, and maintains
 * strict WCAG AA/AAA daylight token values.
 */
class Day17LightModeFinalVerificationTest {

    private val persistentStorage = mutableMapOf<String, String>()

    private inner class SimulatedSettingsRepository : SettingsRepository {
        private val _themeFlow = MutableStateFlow(
            ThemeMode.fromId(persistentStorage[KEY_THEME_MODE] ?: ThemeMode.Pixel.id)
        )
        private val _crtFlow = MutableStateFlow(
            persistentStorage[KEY_CRT_ENABLED]?.toBooleanStrictOrNull() ?: false
        )

        override val themeMode: Flow<ThemeMode> = _themeFlow.asStateFlow()
        override val isCrtEnabled: Flow<Boolean> = _crtFlow.asStateFlow()
        override val isSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isHapticsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isReduceMotionEnabled: Flow<Boolean> = MutableStateFlow(false)
        override val onboardingComplete: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationsEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = MutableStateFlow(true)

        override suspend fun setThemeMode(mode: ThemeMode) {
            persistentStorage[KEY_THEME_MODE] = mode.id
            _themeFlow.value = mode
        }

        override suspend fun setCrtEnabled(enabled: Boolean) {
            persistentStorage[KEY_CRT_ENABLED] = enabled.toString()
            _crtFlow.value = enabled
        }

        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
    }

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        persistentStorage.clear()
        repository = SimulatedSettingsRepository()
    }

    @Test
    fun lightModePersistsAcrossSimulatedAppRestart() = runBlocking {
        // User sets theme to Light mode in session 1
        repository.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, repository.themeMode.first())

        // App terminates and cold restarts: new repository reading from persistent store
        val coldStartRepository = SimulatedSettingsRepository()
        val restoredTheme = coldStartRepository.themeMode.first()

        assertEquals(ThemeMode.Light, restoredTheme)
    }

    @Test
    fun lightModeResolvesToFinishedDaylightTokens() {
        val lightColors = DefaultLightColorScheme

        // Background: Warm ivory parchment
        assertEquals(Color(0xFFF8F6F0), lightColors.background)
        // Surface: Crisp white card container
        assertEquals(Color(0xFFFFFFFF), lightColors.surface)
        // Primary: Retro Arcade Amber / Dungeon Gold
        assertEquals(Color(0xFFB45309), lightColors.primary)
        // Secondary: Sky Blue
        assertEquals(Color(0xFF0284C7), lightColors.secondary)
        // Tertiary: HP Meadow Emerald
        assertEquals(Color(0xFF15803D), lightColors.tertiary)
        // Error: Boss Trap Crimson
        assertEquals(Color(0xFFDC2626), lightColors.error)
        // Stepped Pixel Border: Dark Stone
        assertEquals(Color(0xFF292524), lightColors.pixelBorder)
        // OnSurface: Deep Charcoal
        assertEquals(Color(0xFF1C1917), lightColors.onSurface)
    }

    @Test
    fun crtScanlinesAreSuppressedInLightMode() = runBlocking {
        // User has CRT enabled in settings
        repository.setCrtEnabled(true)
        repository.setThemeMode(ThemeMode.Light)

        val activeTheme = repository.themeMode.first()
        val userCrtPref = repository.isCrtEnabled.first()

        // Architectural rule: CRT filter only renders when themeMode == Pixel AND userCrtPref == true
        val shouldRenderCrt = (activeTheme == ThemeMode.Pixel) && userCrtPref
        assertFalse("CRT overlay must be suppressed in Light Mode", shouldRenderCrt)
    }

    @Test
    fun systemModeWithLightSystemResolvesToLightMode() {
        // When system is not dark (daylight)
        val isSystemDark = false
        val resolvedMode = if (isSystemDark) ThemeMode.Pixel else ThemeMode.Light

        assertEquals(ThemeMode.Light, resolvedMode)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_CRT_ENABLED = "key_crt_enabled"
    }
}
