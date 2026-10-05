package com.pixelquest.app.domain.ai

import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.domain.repository.HabitInsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 41: Unit test verifying that DebugAiInsightTrigger is strictly gated behind BuildConfig.DEBUG.
 * When running in release mode or when isDebug is false, all debug triggers are completely disabled
 * and execute zero repository or network operations.
 */
class DebugAiInsightTriggerGatingTest {

    private class RepositorySpy : HabitInsightRepository {
        var callCount = 0

        override suspend fun generateHabitInsight(forceRefresh: Boolean): GeminiResult<HabitInsightResponse> {
            callCount++
            return GeminiResult.Success(
                HabitInsightResponse(
                    summary = "Heroic day.",
                    suggestion = "Keep going.",
                    encouragement = "Level up soon!"
                )
            )
        }

        override val latestInsight: Flow<HabitInsightResponse?> = flowOf(null)
    }

    @Test
    fun trigger_whenReleaseBuild_isStrictlyDisabled() = runBlocking {
        val repo = RepositorySpy()
        val releaseTrigger = DebugAiInsightTrigger(
            habitInsightRepository = repo,
            clock = { 1000L },
            isDebugProvider = { false } // Simulating release build
        )

        val result = releaseTrigger.triggerDirectly()

        assertFalse("Release execution must not be Success", result.isSuccess)
        assertTrue("Result must be GeminiResult.Disabled", result is GeminiResult.Disabled)
        val disabled = result as GeminiResult.Disabled
        assertTrue(disabled.message.contains("debug builds only"))
        assertEquals("DEBUG_ONLY_RESTRICTION", releaseTrigger.debugStatus.value)
        assertEquals("Repository must NOT be called in release builds", 0, repo.callCount)
    }

    @Test
    fun triggerAsync_whenReleaseBuild_invokesCallbackWithDisabled() = runBlocking {
        val repo = RepositorySpy()
        val releaseTrigger = DebugAiInsightTrigger(
            habitInsightRepository = repo,
            clock = { 1000L },
            isDebugProvider = { false }
        )

        var callbackResult: GeminiResult<HabitInsightResponse>? = null
        releaseTrigger.triggerInsightGeneration { result ->
            callbackResult = result
        }

        assertTrue("Callback must receive GeminiResult.Disabled", callbackResult is GeminiResult.Disabled)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun trigger_whenDebugBuild_executesNormally() = runBlocking {
        val repo = RepositorySpy()
        val debugTrigger = DebugAiInsightTrigger(
            habitInsightRepository = repo,
            clock = { 1000L },
            isDebugProvider = { true } // Simulating debug build
        )

        val result = debugTrigger.triggerDirectly()

        assertTrue("Debug build must allow trigger", result is GeminiResult.Success)
        assertEquals(1, repo.callCount)
        assertEquals("SUCCESS", debugTrigger.debugStatus.value)
    }
}
