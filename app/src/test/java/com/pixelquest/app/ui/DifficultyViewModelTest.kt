package com.pixelquest.app.ui

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.ui.screens.difficulty.DifficultyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeDifficultySettingsRepository : DifficultySettingsRepository {
    private val settingsFlow = MutableStateFlow<DifficultySettingsEntity?>(
        DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f)
    )

    override fun getCurrentDifficulty(): Flow<DifficultySettingsEntity?> = settingsFlow

    override suspend fun updateDifficultySettings(settings: DifficultySettingsEntity) {
        settingsFlow.value = settings
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DifficultyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeDifficultySettingsRepository
    private lateinit var viewModel: DifficultyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeDifficultySettingsRepository()
        viewModel = DifficultyViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onDifficultyClicked_showsWarningDialogAndPendingLevel() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onDifficultyClicked(DifficultyLevel.HARD)

        val state = viewModel.uiState.value
        assertTrue(state.showWarningDialog)
        assertEquals(DifficultyLevel.HARD, state.pendingLevel)
    }

    @Test
    fun confirmDifficultyChange_updatesRepositoryAndState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onDifficultyClicked(DifficultyLevel.HARD)
        viewModel.confirmDifficultyChange()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showWarningDialog)
        assertEquals(DifficultyLevel.HARD, state.currentLevel)

        val repoSettings = fakeRepo.getCurrentDifficulty().first()
        assertEquals(DifficultyLevel.HARD, repoSettings?.difficultyLevel)
        assertEquals(0.90f, repoSettings?.perfectDayThreshold ?: 0f, 0.001f)
    }
}
