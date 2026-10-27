package com.pixelquest.app.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.FakeTaskCompletionRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.FixedClock
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * B05: Verifies that creating a quest never awards XP across all creation code paths.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuestCreateNeverAwardsXpTest {

    private val now = LocalDateTime.of(2026, 10, 27, 10, 0)
    private val appClock = FixedClock(now)

    private lateinit var taskRepository: FakeTaskRepository
    private lateinit var completionRepository: FakeTaskCompletionRepository
    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var streakRepository: FakeStreakRepository
    private lateinit var difficultyRepository: FakeDifficultySettingsRepository
    private lateinit var alarmScheduler: TaskAlarmScheduler

    @Before
    fun setup() {
        taskRepository = FakeTaskRepository()
        completionRepository = FakeTaskCompletionRepository()
        userProfileRepository = FakeUserProfileRepository(
            UserProfileEntity(id = 1, username = "Hero", avatarId = "avatar", level = 1, totalXp = 0),
        )
        streakRepository = FakeStreakRepository()
        difficultyRepository = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7),
        )
        alarmScheduler = TaskAlarmScheduler(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun creatingQuestViaFormViewModel_neverAwardsXp() = runBlocking {
        val formViewModel = TaskFormViewModel(taskRepository, alarmScheduler, appClock)
        formViewModel.onNameChanged("Brand New Quest")
        formViewModel.onTimeSelected(LocalTime.of(12, 0))
        formViewModel.onCategorySelected(TaskCategory.HEALTH)
        formViewModel.onRecurrenceSelected(RecurrenceType.DAILY)

        val initialXp = userProfileRepository.getProfile().first()?.totalXp ?: 0
        assertEquals(0, initialXp)

        var saved = false
        formViewModel.saveTask { saved = true }
        assertTrue(saved)

        // Verify task was inserted
        val allTasks = taskRepository.getAllTasks().first()
        assertEquals(1, allTasks.size)
        assertEquals("Brand New Quest", allTasks.first().name)

        // Verify XP remains 0 and no completion logs were created
        val afterXp = userProfileRepository.getProfile().first()?.totalXp ?: 0
        assertEquals("Creating a quest must never award XP", 0, afterXp)
        val logs = completionRepository.getAllLogs().first()
        assertTrue("No completion logs should be created upon quest creation", logs.isEmpty())
    }

    @Test
    fun creatingQuestDirectlyInRepository_neverAwardsXp() = runBlocking {
        val initialXp = userProfileRepository.getProfile().first()?.totalXp ?: 0

        val insertedId = taskRepository.insertTask(
            TaskEntity(
                name = "Direct Quest",
                description = "Direct insertion",
                scheduledDay = now.toLocalDate(),
                scheduledTime = LocalTime.of(9, 0),
                recurrenceType = RecurrenceType.ONE_TIME,
                category = TaskCategory.FITNESS,
            ),
        )
        assertTrue(insertedId > 0)

        val afterXp = userProfileRepository.getProfile().first()?.totalXp ?: 0
        assertEquals("Direct creation must never award XP", initialXp, afterXp)
        assertTrue(completionRepository.logs.value.isEmpty())
    }

    @Test
    fun editingQuest_neverAwardsXp() = runBlocking {
        val initialTask = TaskEntity(
            id = 5L,
            name = "Initial Quest",
            description = "",
            scheduledDay = now.toLocalDate(),
            scheduledTime = LocalTime.of(9, 0),
            recurrenceType = RecurrenceType.DAILY,
            category = TaskCategory.FITNESS,
        )
        taskRepository.insertTask(initialTask)

        val formViewModel = TaskFormViewModel(taskRepository, alarmScheduler, appClock)
        formViewModel.loadTask(5L)
        formViewModel.onNameChanged("Renamed Quest")

        var saved = false
        formViewModel.saveTask { saved = true }
        assertTrue(saved)

        val afterXp = userProfileRepository.getProfile().first()?.totalXp ?: 0
        assertEquals("Editing a quest must never award XP", 0, afterXp)
        assertTrue(completionRepository.logs.value.isEmpty())
    }
}
