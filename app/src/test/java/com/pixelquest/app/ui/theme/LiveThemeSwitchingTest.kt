package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Step 13 Verification: Verify theme changes apply live reactively
 * without requiring an activity recreation or app restart.
 */
class LiveThemeSwitchingTest {

    private class TestSettingsRepo : SettingsRepository {
        val flow = MutableStateFlow(ThemeMode.Pixel)
        override val themeMode: Flow<ThemeMode> = flow.asStateFlow()
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
            flow.value = mode
        }
    }

    @Test
    fun liveThemeSwitching_updatesReactiveStateContinuouslyWithoutRestart() = runTest {
        val repo = TestSettingsRepo()
        val viewModel = ThemeViewModel(repo)

        // 1. Initially Pixel mode
        assertEquals(ThemeMode.Pixel, viewModel.themeMode.value)
        assertEquals(DefaultPixelColorScheme.primary, DefaultPixelColorScheme.toMaterialColorScheme().primary)

        // 2. Live switch to Light mode
        viewModel.setThemeMode(ThemeMode.Light)
        assertEquals(ThemeMode.Light, viewModel.themeMode.value)
        assertEquals(ThemeMode.Light, repo.themeMode.first())
        assertNotEquals(DefaultLightColorScheme.background, DefaultPixelColorScheme.background)

        // 3. Live switch to Comic mode
        viewModel.setThemeMode(ThemeMode.Comic)
        assertEquals(ThemeMode.Comic, viewModel.themeMode.value)
        assertEquals(ThemeMode.Comic, repo.themeMode.first())

        // 4. Live switch back to Pixel mode
        viewModel.setThemeMode(ThemeMode.Pixel)
        assertEquals(ThemeMode.Pixel, viewModel.themeMode.value)
        assertEquals(ThemeMode.Pixel, repo.themeMode.first())
    }
}
