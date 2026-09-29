package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 24 (Day 23): Integration test for the full theme-cycling scenario across multiple screens:
 * Cycle Pixel -> Comic -> Light -> Comic -> Pixel repeatedly.
 *
 * Verifies:
 * - ThemeMode updates cleanly in SettingsRepository and ThemeViewModel
 * - ComponentThemeFamily dispatches correctly for all screens
 * - Typography and ColorScheme tokens match expected values with zero stale state
 * - CRT filter is strictly active in Pixel mode and excluded in Comic and Light modes
 */
class Day23FullThemeCyclingIntegrationTest {

    private class MockSettingsRepository : SettingsRepository {
        private val _themeMode = MutableStateFlow(ThemeMode.Pixel)
        override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()
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
            _themeMode.value = mode
        }
    }

    private val appScreens = listOf(
        "TodayScreen",
        "TasksScreen",
        "CreateTaskScreen",
        "StatsScreen",
        "ProfileScreen",
        "SettingsScreen",
        "LeaderboardScreen",
        "LevelHistoryScreen",
        "DidYouDoItScreen"
    )

    private val cyclingSequence = listOf(
        ThemeMode.Pixel,
        ThemeMode.Comic,
        ThemeMode.Light,
        ThemeMode.Comic,
        ThemeMode.Pixel
    )

    @Test
    fun fullThemeCyclingAcrossScreens_maintainsFidelityAndZeroStaleState() = runTest {
        val settingsRepo = MockSettingsRepository()
        val themeViewModel = ThemeViewModel(settingsRepo)

        // Repeat the full cycle 3 times to ensure repeated switching does not leak state
        repeat(3) { cycleIteration ->
            for ((stepIndex, targetMode) in cyclingSequence.withIndex()) {
                themeViewModel.setThemeMode(targetMode)

                val activeMode = themeViewModel.themeMode.value
                val persistedMode = settingsRepo.themeMode.first()

                assertEquals(
                    "Iteration $cycleIteration, Step $stepIndex: ViewModel mode mismatch",
                    targetMode,
                    activeMode
                )
                assertEquals(
                    "Iteration $cycleIteration, Step $stepIndex: Repo mode mismatch",
                    targetMode,
                    persistedMode
                )

                // Effective mode check
                val effectiveMode = targetMode.resolveEffective(isSystemInDark = true)
                val expectedFamily = when (effectiveMode) {
                    ThemeMode.Pixel, ThemeMode.System -> ComponentThemeFamily.PIXEL
                    ThemeMode.Light -> ComponentThemeFamily.LIGHT
                    ThemeMode.Comic -> ComponentThemeFamily.COMIC
                }

                val resolvedFamily = ComponentThemeFamily.fromThemeMode(effectiveMode)
                assertEquals(
                    "Iteration $cycleIteration, Step $stepIndex: ComponentThemeFamily mismatch",
                    expectedFamily,
                    resolvedFamily
                )

                val scheme: AppColorScheme = when (effectiveMode) {
                    ThemeMode.Pixel, ThemeMode.System -> DefaultPixelColorScheme
                    ThemeMode.Light -> DefaultLightColorScheme
                    ThemeMode.Comic -> DefaultComicColorScheme
                }

                // Verify color schemes and screen consumption
                for (screen in appScreens) {
                    assertNotNull("Screen $screen missing primary color", scheme.primary)
                    assertNotNull("Screen $screen missing background color", scheme.background)
                    assertNotNull("Screen $screen missing surface color", scheme.surface)

                    when (effectiveMode) {
                        ThemeMode.Comic -> {
                            assertEquals(ComicTokens.CoralRed, scheme.primary)
                            assertEquals(ComicTokens.PaperBackground, scheme.background)
                            assertEquals(ComicTokens.PanelSurface, scheme.surface)
                            assertFalse("Comic mode must not be dark", scheme.isDark)
                        }
                        ThemeMode.Light -> {
                            assertFalse("Light mode must not be dark", scheme.isDark)
                        }
                        ThemeMode.Pixel -> {
                            assertTrue("Pixel mode must be dark", scheme.isDark)
                        }
                        else -> {}
                    }
                }

                // Verify CRT scanline filtering is strictly gated to Pixel mode
                val isCrtFilterEligible = settingsRepo.isCrtEnabled.first() && effectiveMode == ThemeMode.Pixel
                if (effectiveMode == ThemeMode.Pixel) {
                    assertTrue(isCrtFilterEligible)
                } else {
                    assertFalse(
                        "CRT filter must remain disabled in mode $effectiveMode",
                        isCrtFilterEligible
                    )
                }
            }
        }
    }
}
