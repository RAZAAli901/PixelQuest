package com.pixelquest.app.domain

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.screens.difficulty.DifficultyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Step 28: Unit test verifying difficulty-lock logic under Simple Mode.
 * Ensures difficulty modifications are rejected/locked while Simple Mode is active,
 * and permitted normally when Simple Mode is inactive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DifficultyLockUnderSimpleModeTest {

    private inner class FakeSettingsRepo(initialSimpleMode: Boolean = false) : SettingsRepository {
        val simpleModeFlow = MutableStateFlow(initialSimpleMode)

        override val simpleModeEnabled: Flow<Boolean> = simpleModeFlow.asStateFlow()
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

        override suspend fun setSimpleModeEnabled(enabled: Boolean) {
            simpleModeFlow.value = enabled
        }
    }

    private inner class FakeDifficultyRepo : DifficultySettingsRepository {
        val difficultyFlow = MutableStateFlow<DifficultySettingsEntity?>(
            DifficultySettingsEntity(
                id = 1,
                difficultyLevel = DifficultyLevel.MEDIUM,
                perfectDayThreshold = 0.70f,
                daysRequiredPerLevel = 7
            )
        )

        override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = difficultyFlow

        override suspend fun insertSettings(settings: DifficultySettingsEntity) {
            difficultyFlow.value = settings
        }

        override suspend fun updateSettings(settings: DifficultySettingsEntity) {
            difficultyFlow.value = settings
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepo: FakeSettingsRepo
    private lateinit var difficultyRepo: FakeDifficultyRepo
    private lateinit var viewModel: DifficultyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepo = FakeSettingsRepo(initialSimpleMode = false)
        difficultyRepo = FakeDifficultyRepo()
        viewModel = DifficultyViewModel(
            difficultySettingsRepository = difficultyRepo,
            settingsRepository = settingsRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGamifiedMode_allowsDifficultyChanges() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSimpleModeEnabled)

        // Select HARD difficulty
        viewModel.onDifficultyClicked(DifficultyLevel.HARD)
        val pendingState = viewModel.uiState.value
        assertTrue(pendingState.showWarningDialog)
        assertEquals(DifficultyLevel.HARD, pendingState.pendingLevel)

        // Confirm
        viewModel.confirmDifficultyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        val confirmedState = viewModel.uiState.value
        assertFalse(confirmedState.showWarningDialog)
        assertEquals(DifficultyLevel.HARD, confirmedState.currentLevel)
        assertEquals(DifficultyLevel.HARD, difficultyRepo.getCurrentDifficulty().first()?.difficultyLevel)
    }

    @Test
    fun testSimpleMode_locksDifficulty_rejectsClickAndConfirmation() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Enable Simple Mode
        settingsRepo.setSimpleModeEnabled(true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSimpleModeEnabled)
        assertEquals(DifficultyLevel.MEDIUM, viewModel.uiState.value.currentLevel)

        // Attempt to select EASY
        viewModel.onDifficultyClicked(DifficultyLevel.EASY)
        val stateAfterClick = viewModel.uiState.value
        assertFalse("Warning dialog should not show when locked", stateAfterClick.showWarningDialog)
        assertNull("Pending level must remain null when locked", stateAfterClick.pendingLevel)

        // Attempt to select HARD
        viewModel.onDifficultyClicked(DifficultyLevel.HARD)
        val stateAfterHardClick = viewModel.uiState.value
        assertFalse(stateAfterHardClick.showWarningDialog)
        assertNull(stateAfterHardClick.pendingLevel)

        // Attempt to confirm difficulty change directly
        viewModel.confirmDifficultyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert difficulty repository remains unchanged at MEDIUM
        assertEquals(DifficultyLevel.MEDIUM, difficultyRepo.getCurrentDifficulty().first()?.difficultyLevel)
        assertEquals(DifficultyLevel.MEDIUM, viewModel.uiState.value.currentLevel)
    }
}
