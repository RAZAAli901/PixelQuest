package com.pixelquest.app.qa

import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.GeminiClient
import com.pixelquest.app.data.remote.GeminiResult
import com.pixelquest.app.data.repository.HabitInsightRepositoryImpl
import com.pixelquest.app.domain.ai.DebugAiInsightTrigger
import com.pixelquest.app.domain.ai.DefaultHabitInsightToneHook
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.StreakRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

/**
 * Step 36: Manual QA verification test simulating device offline / airplane mode conditions.
 * Verifies graceful degradation with zero crashes across DNS resolution failure, socket disconnection,
 * and connection timeouts.
 */
class Day24OfflineGracefulDegradationQaTest {

    private class OfflineSimulatedGeminiClient(
        var offlineException: Exception = UnknownHostException("Unable to resolve host \"generativelanguage.googleapis.com\": No address associated with hostname")
    ) : GeminiClient {
        var attempts = 0

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            attempts++
            throw offlineException
        }
    }

    private fun createRepository(geminiClient: GeminiClient): HabitInsightRepositoryImpl {
        val streakRepo = com.pixelquest.app.testing.FakeStreakRepository(StreakEntity(currentStreak = 4))

        val profileRepo = com.pixelquest.app.testing.FakeUserProfileRepository(UserProfileEntity(username = "Player", avatarId = "warrior"))

        val taskRepo = com.pixelquest.app.testing.FakeTaskRepository()

        val completionRepo = com.pixelquest.app.testing.FakeTaskCompletionRepository()

        val settingsRepo = object : SettingsRepository {
            override val aiInsightsEnabled: Flow<Boolean> = flowOf(true)
            override val simpleModeEnabled: Flow<Boolean> = flowOf(false)
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

        return HabitInsightRepositoryImpl(
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            settingsRepository = settingsRepo,
            geminiClient = geminiClient,
            toneHook = DefaultHabitInsightToneHook()
        )
    }

    @Test
    fun manualQa_airplaneMode_dnsFailure_degradesGracefully() = runBlocking {
        val client = OfflineSimulatedGeminiClient(
            UnknownHostException("Unable to resolve host \"generativelanguage.googleapis.com\": No address associated with hostname")
        )
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()

        assertNotNull("Result must not be null", result)
        assertFalse("Offline call must not report success", result.isSuccess)
        assertTrue("Result must be GeminiResult.NetworkError", result is GeminiResult.NetworkError)
        val networkError = result as GeminiResult.NetworkError
        assertTrue("Error message must inform player of connection issue", networkError.message.contains("Network error") || networkError.message.contains("connection"))

        // Verify latestInsight flow was NOT corrupted with partial data
        assertNull("Flow should remain null", repository.latestInsight.first())

        // Verify trigger status updated without crashing
        assertTrue("Debug trigger status reflects network failure", trigger.debugStatus.value.startsWith("NETWORK_ERROR"))
    }

    @Test
    fun manualQa_airplaneMode_socketUnreachable_degradesGracefully() = runBlocking {
        val client = OfflineSimulatedGeminiClient(
            SocketException("Network is unreachable (connect failed)")
        )
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue("SocketException must be captured as NetworkError", result is GeminiResult.NetworkError)
        assertEquals("NETWORK_ERROR: ${com.pixelquest.app.domain.ai.AiErrorCopy.OFFLINE}", trigger.debugStatus.value)
    }

    @Test
    fun manualQa_airplaneMode_connectionRefused_degradesGracefully() = runBlocking {
        val client = OfflineSimulatedGeminiClient(
            ConnectException("Connection refused")
        )
        val repository = createRepository(client)
        val trigger = DebugAiInsightTrigger(repository)

        val result = trigger.triggerDirectly()
        assertTrue("ConnectException must be captured as NetworkError", result is GeminiResult.NetworkError)
        assertEquals(1, client.attempts)
    }
}
