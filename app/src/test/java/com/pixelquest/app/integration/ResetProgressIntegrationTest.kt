package com.pixelquest.app.integration

import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.Priority
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskDifficulty
import com.pixelquest.app.domain.repository.DifficultySettingsRepository
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ResetProgressIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val tasksList = mutableListOf(
        TaskEntity(1, "Task 1", "Desc", LocalTime.now(), LocalDate.now(), RecurrenceType.DAILY, Priority.HIGH, TaskDifficulty.HARD, false)
    )
    private var savedProfile = UserProfileEntity(1, "OldHero", "avatar_ninja", 5, 500, 3)
    private var savedDifficulty = DifficultySettingsEntity(1, DifficultyLevel.HARD, 0.9f, 10)
    private var onboardingCompleted = true

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val fakeUserRepo = object : UserProfileRepository {
            override fun getProfile() = flowOf(savedProfile)
            override suspend fun saveProfile(profile: UserProfileEntity) {
                savedProfile = profile
            }
        }

        val fakeDiffRepo = object : DifficultySettingsRepository {
            override fun getCurrentDifficulty() = flowOf(savedDifficulty)
            override suspend fun updateDifficultySettings(settings: DifficultySettingsEntity) {
                savedDifficulty = settings
            }
        }

        val fakeTaskRepo = object : TaskRepository {
            override fun getAllTasks() = flowOf(tasksList.toList())
            override fun getTaskById(id: Long) = flowOf(tasksList.find { it.id == id })
            override suspend fun insertTask(task: TaskEntity): Long {
                tasksList.add(task)
                return task.id
            }
            override suspend fun updateTask(task: TaskEntity) {}
            override suspend fun deleteTask(task: TaskEntity) {
                tasksList.remove(task)
            }
        }

        val fakeSettingsRepo = object : SettingsRepository {
            override val isSoundEnabled: Flow<Boolean> = flowOf(true)
            override val isCrtEnabled: Flow<Boolean> = flowOf(false)
            override val onboardingComplete: Flow<Boolean> = flowOf(onboardingCompleted)
            override val isNotificationsEnabled: Flow<Boolean> = flowOf(true)
            override val isNotificationSoundEnabled: Flow<Boolean> = flowOf(true)
            override val isNotificationVibrationEnabled: Flow<Boolean> = flowOf(true)
            override suspend fun setSoundEnabled(enabled: Boolean) {}
            override suspend fun setCrtEnabled(enabled: Boolean) {}
            override suspend fun setOnboardingComplete(complete: Boolean) {
                onboardingCompleted = complete
            }
            override suspend fun setNotificationsEnabled(enabled: Boolean) {}
            override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
            override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}
        }

        val mockContext = org.mockito.Mockito.mock(android.content.Context::class.java)
        val alarmManager = org.mockito.Mockito.mock(android.app.AlarmManager::class.java)
        org.mockito.Mockito.`when`(mockContext.getSystemService(android.content.Context.ALARM_SERVICE)).thenReturn(alarmManager)
        val scheduler = TaskAlarmScheduler(mockContext)

        viewModel = SettingsViewModel(
            fakeSettingsRepo,
            fakeUserRepo,
            fakeDiffRepo,
            fakeTaskRepo,
            scheduler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPerformFullResetWipesDataAndResetsOnboarding() = runTest {
        var callbackFired = false
        viewModel.performFullReset {
            callbackFired = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackFired)
        assertFalse(onboardingCompleted)
        assertTrue(tasksList.isEmpty())
        assertEquals("PixelHero", savedProfile.username)
        assertEquals("avatar_hero", savedProfile.avatarId)
    }
}
