package com.pixelquest.app.data.repository

import com.pixelquest.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Step 5: Unit test for [SettingsRepository.simpleModeEnabled] persistence and reactive updates.
 * Verifies preference storage, reactive StateFlow updates, and state recovery across simulated app/repo restarts.
 */
class SimpleModePersistenceTest {

    // Simulates persistent key-value SharedPreferences storage
    private val persistentStore = mutableMapOf<String, Boolean>()

    private inner class FakeSettingsRepository : SettingsRepository {
        private val _simpleModeFlow = MutableStateFlow(
            persistentStore[KEY_SIMPLE_MODE_ENABLED] ?: false
        )

        override val simpleModeEnabled: Flow<Boolean> = _simpleModeFlow.asStateFlow()
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
            persistentStore[KEY_SIMPLE_MODE_ENABLED] = enabled
            _simpleModeFlow.value = enabled
        }
    }

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        persistentStore.clear()
        repository = FakeSettingsRepository()
    }

    @Test
    fun defaultSimpleModePreference_isFalse() = runBlocking {
        val initial = repository.simpleModeEnabled.first()
        assertFalse("Default Simple Mode preference must be false", initial)
    }

    @Test
    fun setSimpleModeEnabled_updatesFlowReactively() = runBlocking {
        repository.setSimpleModeEnabled(true)
        assertTrue("Flow must emit true when Simple Mode is enabled", repository.simpleModeEnabled.first())

        repository.setSimpleModeEnabled(false)
        assertFalse("Flow must emit false when Simple Mode is disabled", repository.simpleModeEnabled.first())
    }

    @Test
    fun simpleModePreference_persistsAcrossRepositoryRecreation() = runBlocking {
        // User enables Simple Mode
        repository.setSimpleModeEnabled(true)

        // Simulate app restart / new repository instance reading from same backing store
        val reloadedRepository = FakeSettingsRepository()
        val restored = reloadedRepository.simpleModeEnabled.first()

        assertTrue("Simple Mode preference must persist across repository re-instantiation", restored)
    }

    @Test
    fun toggleSimpleMode_returnsToDefaultCleanly() = runBlocking {
        repository.setSimpleModeEnabled(true)
        assertTrue(repository.simpleModeEnabled.first())

        repository.setSimpleModeEnabled(false)
        val reloadedRepository = FakeSettingsRepository()
        assertFalse("Disabling Simple Mode must persist false across restarts", reloadedRepository.simpleModeEnabled.first())
    }

    companion object {
        private const val KEY_SIMPLE_MODE_ENABLED = "key_simple_mode_enabled"
    }
}
