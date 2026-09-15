package com.pixelquest.app.ui.theme

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSettingsRepository : SettingsRepository {
        val themeModeFlow = MutableStateFlow(ThemeMode.Pixel)

        override val themeMode: Flow<ThemeMode> = themeModeFlow.asStateFlow()
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
            themeModeFlow.value = mode
        }
    }

    private lateinit var fakeRepo: FakeSettingsRepository
    private lateinit var viewModel: ThemeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepository()
        viewModel = ThemeViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialThemeModeIsPixel() {
        assertEquals(ThemeMode.Pixel, viewModel.themeMode.value)
    }

    @Test
    fun setThemeModeUpdatesViewModelState() = runTest {
        viewModel.setThemeMode(ThemeMode.Light)
        advanceUntilIdle()
        assertEquals(ThemeMode.Light, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.Comic)
        advanceUntilIdle()
        assertEquals(ThemeMode.Comic, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.Pixel)
        advanceUntilIdle()
        assertEquals(ThemeMode.Pixel, viewModel.themeMode.value)
    }

    @Test
    fun repositoryExternalChangeReflectsInViewModel() = runTest {
        fakeRepo.themeModeFlow.value = ThemeMode.Light
        advanceUntilIdle()
        assertEquals(ThemeMode.Light, viewModel.themeMode.value)
    }
}
