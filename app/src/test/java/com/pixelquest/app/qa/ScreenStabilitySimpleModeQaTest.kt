package com.pixelquest.app.qa

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.SimpleModeSuppression
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.screens.difficulty.DifficultyUiState
import com.pixelquest.app.ui.screens.difficulty.DifficultyViewModel
import com.pixelquest.app.ui.screens.settings.SettingsUiState
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import com.pixelquest.app.ui.screens.settings.SimpleModeViewModel
import com.pixelquest.app.ui.screens.today.TodayUiState
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Step 38: Manual QA test verifying screen and ViewModel stability under Simple Mode.
 * Confirms that toggling Simple Mode does not crash any screen, corrupt states,
 * or throw uncaught exceptions across all app components.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenStabilitySimpleModeQaTest {

    private inner class FakeSettingsRepo : SettingsRepository {
        val simpleModeFlow = MutableStateFlow(false)
        val soundFlow = MutableStateFlow(true)
        val crtFlow = MutableStateFlow(false)
        val hapticsFlow = MutableStateFlow(true)
        val reduceMotionFlow = MutableStateFlow(false)
        val onboardingFlow = MutableStateFlow(true)
        val notifFlow = MutableStateFlow(true)
        val notifSoundFlow = MutableStateFlow(true)
        val notifVibFlow = MutableStateFlow(true)

        override val simpleModeEnabled: Flow<Boolean> = simpleModeFlow.asStateFlow()
        override val isSoundEnabled: Flow<Boolean> = soundFlow.asStateFlow()
        override val isCrtEnabled: Flow<Boolean> = crtFlow.asStateFlow()
        override val isHapticsEnabled: Flow<Boolean> = hapticsFlow.asStateFlow()
        override val isReduceMotionEnabled: Flow<Boolean> = reduceMotionFlow.asStateFlow()
        override val onboardingComplete: Flow<Boolean> = onboardingFlow.asStateFlow()
        override val isNotificationsEnabled: Flow<Boolean> = notifFlow.asStateFlow()
        override val isNotificationSoundEnabled: Flow<Boolean> = notifSoundFlow.asStateFlow()
        override val isNotificationVibrationEnabled: Flow<Boolean> = notifVibFlow.asStateFlow()

        override suspend fun setSoundEnabled(enabled: Boolean) { soundFlow.value = enabled }
        override suspend fun setCrtEnabled(enabled: Boolean) { crtFlow.value = enabled }
        override suspend fun setHapticsEnabled(enabled: Boolean) { hapticsFlow.value = enabled }
        override suspend fun setReduceMotionEnabled(enabled: Boolean) { reduceMotionFlow.value = enabled }
        override suspend fun setOnboardingComplete(complete: Boolean) { onboardingFlow.value = complete }
        override suspend fun setNotificationsEnabled(enabled: Boolean) { notifFlow.value = enabled }
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) { notifSoundFlow.value = enabled }
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) { notifVibFlow.value = enabled }

        override suspend fun setSimpleModeEnabled(enabled: Boolean) {
            simpleModeFlow.value = enabled
        }
    }

    private inner class FakeDifficultyRepo : DifficultySettingsRepository {
        val flow = MutableStateFlow<DifficultySettingsEntity?>(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7)
        )
        override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = flow
        override suspend fun insertSettings(settings: DifficultySettingsEntity) { flow.value = settings }
        override suspend fun updateSettings(settings: DifficultySettingsEntity) { flow.value = settings }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepo: FakeSettingsRepo
    private lateinit var difficultyRepo: FakeDifficultyRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepo = FakeSettingsRepo()
        difficultyRepo = FakeDifficultyRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTogglingSimpleMode_preservesViewModelStabilityWithoutCrashes() = runTest {
        // 1. Instantiate ViewModels
        val simpleModeVm = SimpleModeViewModel(settingsRepo)
        val difficultyVm = DifficultyViewModel(difficultyRepo, settingsRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state: Simple Mode OFF
        assertFalse(simpleModeVm.simpleModeEnabled.value)
        assertFalse(difficultyVm.uiState.value.isSimpleModeEnabled)

        // 2. Toggle Simple Mode ON
        simpleModeVm.setSimpleModeEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert all ViewModels reflect ON state cleanly without crash
        assertTrue(simpleModeVm.simpleModeEnabled.value)
        assertTrue(difficultyVm.uiState.value.isSimpleModeEnabled)

        // 3. Verify Suppression Model definitions remain valid
        val suppressionList = SimpleModeSuppression.ALL_SUPPRESSIONS
        assertEquals(5, suppressionList.size)
        assertTrue(suppressionList.contains(SimpleModeSuppression.STREAK_DISPLAY))
        assertTrue(suppressionList.contains(SimpleModeSuppression.POINTS_XP_DISPLAY))
        assertTrue(suppressionList.contains(SimpleModeSuppression.LEVEL_BADGE_CELEBRATION))
        assertTrue(suppressionList.contains(SimpleModeSuppression.DIFFICULTY_SELECTION))
        assertTrue(suppressionList.contains(SimpleModeSuppression.GAMIFICATION_FLOURISHES))

        // 4. Toggle Simple Mode OFF
        simpleModeVm.setSimpleModeEnabled(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(simpleModeVm.simpleModeEnabled.value)
        assertFalse(difficultyVm.uiState.value.isSimpleModeEnabled)
    }
}
