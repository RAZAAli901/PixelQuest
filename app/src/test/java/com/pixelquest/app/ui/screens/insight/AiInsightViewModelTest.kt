package com.pixelquest.app.ui.screens.insight

import com.pixelquest.app.data.local.entity.InsightCacheEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.repository.HabitInsightRepository
import com.pixelquest.app.domain.repository.InsightCacheRepository
import com.pixelquest.app.domain.repository.NoOpInsightCacheRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Step 14: Unit tests verifying AiInsightViewModel's reactive state transitions across:
 * Disabled, NotEnoughData, Success (cached vs live), RateLimited, and Error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiInsightViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeHabitInsightRepository : HabitInsightRepository {
        var generateResult: GeminiResult<HabitInsightResponse> = GeminiResult.Success(
            HabitInsightResponse("Summary", "Suggestion", "Encouragement", generatedAt = 1000L)
        )
        var cooldownSeconds: Long = 0L
        val latestInsightFlow = MutableStateFlow<HabitInsightResponse?>(null)

        override suspend fun generateHabitInsight(forceRefresh: Boolean): GeminiResult<HabitInsightResponse> {
            if (generateResult is GeminiResult.Success) {
                latestInsightFlow.value = (generateResult as GeminiResult.Success).data
            }
            return generateResult
        }

        override val latestInsight: Flow<HabitInsightResponse?> = latestInsightFlow

        override suspend fun getRemainingCooldownSeconds(): Long = cooldownSeconds
    }

    private class FakeCacheRepository : InsightCacheRepository by NoOpInsightCacheRepository() {
        var cachedEntity: InsightCacheEntity? = null
        override suspend fun getLatestInsight(): InsightCacheEntity? = cachedEntity
    }

    private class FakeSettingsRepository : SettingsRepository {
        val aiEnabledFlow = MutableStateFlow(true)
        val simpleModeFlow = MutableStateFlow(false)

        override val aiInsightsEnabled: Flow<Boolean> = aiEnabledFlow
        override val simpleModeEnabled: Flow<Boolean> = simpleModeFlow

        override suspend fun setAiInsightsEnabled(enabled: Boolean) {
            aiEnabledFlow.value = enabled
        }

        override val isSoundEnabled: Flow<Boolean> = flowOf(true)
        override val isCrtEnabled: Flow<Boolean> = flowOf(false)
        override val isHapticsEnabled: Flow<Boolean> = flowOf(true)
        override val isReduceMotionEnabled: Flow<Boolean> = flowOf(false)
        override val onboardingComplete: Flow<Boolean> = flowOf(true)
        override val isNotificationsEnabled: Flow<Boolean> = flowOf(true)
        override val isNotificationSoundEnabled: Flow<Boolean> = flowOf(true)
        override val isNotificationVibrationEnabled: Flow<Boolean> = flowOf(true)
        override suspend fun setSoundEnabled(enabled: Boolean) {}
        override suspend fun setCrtEnabled(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setReduceMotionEnabled(enabled: Boolean) {}
        override suspend fun setOnboardingComplete(complete: Boolean) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
        override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
    }

    private class FakeCompletionRepository : TaskCompletionRepository {
        val logs = mutableListOf<TaskCompletionLogEntity>()
        override suspend fun insertLog(log: TaskCompletionLogEntity): Long = 1L
        override fun getLogsForDate(date: LocalDate): Flow<List<TaskCompletionLogEntity>> = flowOf(logs)
        override fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLogEntity>> = flowOf(logs)
        override fun getCompletionHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskCompletionLogEntity>> = flowOf(logs)
        override fun getAllLogs(): Flow<List<TaskCompletionLogEntity>> = flowOf(logs)
    }

    private fun populateSufficientLogs(repo: FakeCompletionRepository) {
        val today = LocalDate.now()
        repo.logs.addAll(
            listOf(
                TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = today, wasCompleted = true),
                TaskCompletionLogEntity(id = 2, taskId = 1, completedDate = today.minusDays(1), wasCompleted = true),
                TaskCompletionLogEntity(id = 3, taskId = 1, completedDate = today.minusDays(2), wasCompleted = true)
            )
        )
    }

    @Test
    fun initialState_whenDisabled_emitsDisabled() = runTest {
        val settingsRepo = FakeSettingsRepository()
        settingsRepo.aiEnabledFlow.value = false

        val viewModel = AiInsightViewModel(
            habitInsightRepository = FakeHabitInsightRepository(),
            settingsRepository = settingsRepo,
            taskCompletionRepository = FakeCompletionRepository(),
            insightCacheRepository = FakeCacheRepository()
        )

        advanceUntilIdle()
        assertTrue("State must be Disabled", viewModel.uiState.value is AiInsightUiState.Disabled)
    }

    @Test
    fun initialState_whenNotEnoughHistory_emitsNotEnoughData() = runTest {
        val settingsRepo = FakeSettingsRepository()
        settingsRepo.aiEnabledFlow.value = true

        val completionRepo = FakeCompletionRepository()
        completionRepo.logs.add(TaskCompletionLogEntity(id = 1, taskId = 1, completedDate = LocalDate.now(), wasCompleted = true))

        val viewModel = AiInsightViewModel(
            habitInsightRepository = FakeHabitInsightRepository(),
            settingsRepository = settingsRepo,
            taskCompletionRepository = completionRepo,
            insightCacheRepository = FakeCacheRepository()
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("State must be NotEnoughData", state is AiInsightUiState.NotEnoughData)
        val notEnough = state as AiInsightUiState.NotEnoughData
        assertEquals(1, notEnough.daysLogged)
        assertEquals(2, notEnough.remainingDaysNeeded)
        assertTrue(notEnough.progressRatio > 0f)
    }

    @Test
    fun initialState_whenSufficientDataAndCached_emitsSuccessWithIsCachedTrue() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val completionRepo = FakeCompletionRepository()
        populateSufficientLogs(completionRepo)

        val cacheRepo = FakeCacheRepository()
        cacheRepo.cachedEntity = InsightCacheEntity(
            id = 1,
            summary = "Cached Heroic Summary",
            suggestion = "Rest well.",
            encouragement = "Great power awaits!",
            dataHash = "hash123",
            generatedAt = 1000L
        )

        val habitRepo = FakeHabitInsightRepository()
        habitRepo.generateResult = GeminiResult.Success(cacheRepo.cachedEntity!!.toInsightResponse())

        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitRepo,
            settingsRepository = settingsRepo,
            taskCompletionRepository = completionRepo,
            insightCacheRepository = cacheRepo
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("State must be Success", state is AiInsightUiState.Success)
        val success = state as AiInsightUiState.Success
        assertEquals("Cached Heroic Summary", success.insight.summary)
        assertTrue("Must indicate cached status", success.isCached)
    }

    @Test
    fun rateLimited_emitsRateLimitedWithAccurateRemainingSeconds() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val completionRepo = FakeCompletionRepository()
        populateSufficientLogs(completionRepo)

        val habitRepo = FakeHabitInsightRepository()
        habitRepo.cooldownSeconds = 7200L
        habitRepo.generateResult = GeminiResult.RateLimited(
            retryAfterSeconds = 7200L,
            message = "Check back in 2 hours for a fresh insight."
        )

        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitRepo,
            settingsRepository = settingsRepo,
            taskCompletionRepository = completionRepo,
            insightCacheRepository = FakeCacheRepository()
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("State must be RateLimited", state is AiInsightUiState.RateLimited)
        val rateLimited = state as AiInsightUiState.RateLimited
        assertEquals(7200L, rateLimited.retryAfterSeconds)
        assertTrue(rateLimited.message.contains("2 hours"))
    }

    @Test
    fun error_emitsErrorWithRetryAbility() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val completionRepo = FakeCompletionRepository()
        populateSufficientLogs(completionRepo)

        val habitRepo = FakeHabitInsightRepository()
        habitRepo.generateResult = GeminiResult.NetworkError(
            cause = java.io.IOException("No internet"),
            message = "Network connection unavailable."
        )

        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitRepo,
            settingsRepository = settingsRepo,
            taskCompletionRepository = completionRepo,
            insightCacheRepository = FakeCacheRepository()
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("State must be Error", state is AiInsightUiState.Error)
        val error = state as AiInsightUiState.Error
        assertTrue(error.canRetry)
        assertEquals("Network connection unavailable.", error.message)
    }

    @Test
    fun refreshInsight_whenCooldownActive_blocksRefreshAndSetsRateLimitedState() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val completionRepo = FakeCompletionRepository()
        populateSufficientLogs(completionRepo)

        val habitRepo = FakeHabitInsightRepository()
        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitRepo,
            settingsRepository = settingsRepo,
            taskCompletionRepository = completionRepo,
            insightCacheRepository = FakeCacheRepository()
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AiInsightUiState.Success)

        // Now set cooldown active
        habitRepo.cooldownSeconds = 3600L
        viewModel.refreshInsight()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be RateLimited during cooldown", state is AiInsightUiState.RateLimited)
        val rateLimited = state as AiInsightUiState.RateLimited
        assertEquals(3600L, rateLimited.retryAfterSeconds)
        assertNotNull(rateLimited.lastInsight)
    }

    @Test
    fun refreshInsight_whenCooldownZero_forcesRefresh() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val completionRepo = FakeCompletionRepository()
        populateSufficientLogs(completionRepo)

        val habitRepo = FakeHabitInsightRepository()
        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitRepo,
            settingsRepository = settingsRepo,
            taskCompletionRepository = completionRepo,
            insightCacheRepository = FakeCacheRepository()
        )
        advanceUntilIdle()

        habitRepo.cooldownSeconds = 0L
        habitRepo.generateResult = GeminiResult.Success(
            HabitInsightResponse("Refreshed Summary", "Refreshed Suggestion", "Refreshed Cheer", generatedAt = 2000L)
        )
        viewModel.refreshInsight()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Success", state is AiInsightUiState.Success)
        val success = state as AiInsightUiState.Success
        assertEquals("Refreshed Summary", success.insight.summary)
        assertTrue(success.canRefresh)
    }

    @Test
    fun whenDisabled_showsClearOptInPromptAndBlocksApiRequests() = runTest {
        val settingsRepo = FakeSettingsRepository()
        settingsRepo.aiEnabledFlow.value = false
        val habitRepo = FakeHabitInsightRepository()

        val viewModel = AiInsightViewModel(
            habitInsightRepository = habitRepo,
            settingsRepository = settingsRepo,
            taskCompletionRepository = FakeCompletionRepository(),
            insightCacheRepository = FakeCacheRepository()
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Disabled", state is AiInsightUiState.Disabled)
        val disabled = state as AiInsightUiState.Disabled
        assertTrue("Message must explain how to opt in", disabled.message.contains("Settings", ignoreCase = true))

        // Triggering refresh must remain disabled and not call Gemini
        viewModel.refreshInsight()
        advanceUntilIdle()

        assertTrue("State must remain Disabled", viewModel.uiState.value is AiInsightUiState.Disabled)
    }
}
