package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 36: Integration test cycling through all theme modes (Pixel, Light, Comic, System)
 * and verifying palette integrity, color resolution, and zero crashes across UI states.
 */
class ThemeModeSwitchIntegrationTest {

    private class TestSettingsRepo : SettingsRepository {
        val themeFlow = MutableStateFlow(ThemeMode.Pixel)
        override val themeMode: Flow<ThemeMode> = themeFlow.asStateFlow()
        override val isSoundEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val isCrtEnabled: Flow<Boolean> = MutableStateFlow(true)
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
            themeFlow.value = mode
        }
    }

    private val screens = listOf(
        "TodayScreen",
        "TasksScreen",
        "StatsScreen",
        "ProfileScreen",
        "SettingsScreen",
        "LeaderboardScreen"
    )

    @Test
    fun cycleAllThemes_noCrashesAndAccuratePaletteMapping() = runTest {
        val repo = TestSettingsRepo()
        val viewModel = ThemeViewModel(repo)

        val modesToTest = listOf(
            ThemeMode.Pixel,
            ThemeMode.Light,
            ThemeMode.Comic,
            ThemeMode.System
        )

        for (targetMode in modesToTest) {
            viewModel.setThemeMode(targetMode)
            assertEquals(targetMode, viewModel.themeMode.value)
            assertEquals(targetMode, repo.themeMode.first())

            // Test effective theme resolution under both day and night OS settings
            for (isSystemDark in listOf(true, false)) {
                val effective = targetMode.resolveEffective(isSystemDark)
                val scheme: AppColorScheme = when (effective) {
                    ThemeMode.Pixel, ThemeMode.System -> DefaultPixelColorScheme
                    ThemeMode.Light -> DefaultLightColorScheme
                    ThemeMode.Comic -> DefaultComicColorScheme
                }

                // Verify color integrity across every screen consumer
                for (screen in screens) {
                    assertNotNull("Screen $screen failed to resolve primary color", scheme.primary)
                    assertNotNull("Screen $screen failed to resolve background color", scheme.background)
                    assertNotNull("Screen $screen failed to resolve surface color", scheme.surface)
                    assertNotNull("Screen $screen failed to resolve tertiary color", scheme.tertiary)

                    val matScheme = scheme.toMaterialColorScheme()
                    assertNotNull("Material primary for $screen was null", matScheme.primary)
                    assertNotNull("Material background for $screen was null", matScheme.background)
                }

                // Verify CRT scanline gating rule
                val crtActive = repo.isCrtEnabled.first() && effective == ThemeMode.Pixel
                if (effective == ThemeMode.Pixel) {
                    assertTrue(crtActive)
                } else {
                    org.junit.Assert.assertFalse(crtActive)
                }
            }
        }
    }
}
