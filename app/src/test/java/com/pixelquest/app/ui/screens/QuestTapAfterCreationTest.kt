package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.domain.TaskResultRecorder
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
import com.pixelquest.app.ui.screens.history.TaskHistoryViewModel
import com.pixelquest.app.ui.screens.tasks.TaskFormViewModel
import com.pixelquest.app.ui.screens.tasks.TaskViewModel
import com.pixelquest.app.ui.screens.today.TodayViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * B02 & B04: Verifies a just-created quest is tappable immediately,
 * after rotation, and after the app restarts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuestTapAfterCreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = LocalDateTime.of(2026, 10, 27, 10, 0)
    private val appClock = FixedClock(now)

    private fun setupTestBench(): Pair<FakeTaskRepository, FakeTaskCompletionRepository> {
        val taskRepo = FakeTaskRepository()
        val completionRepo = FakeTaskCompletionRepository()
        return taskRepo to completionRepo
    }

    private fun buildQuestsScreen(
        taskRepo: FakeTaskRepository,
        completionRepo: FakeTaskCompletionRepository,
        onEditQuest: (Long) -> Unit = {},
    ): Triple<TodayViewModel, TaskViewModel, TaskHistoryViewModel> {
        val profileRepo = FakeUserProfileRepository()
        val streakRepo = FakeStreakRepository()
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val alarmScheduler = TaskAlarmScheduler(context)

        val todayViewModel = TodayViewModel(
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            streakRepository = streakRepo,
            userProfileRepository = profileRepo,
            difficultySettingsRepository = diffRepo,
            taskAlarmScheduler = alarmScheduler,
            taskResultRecorder = TaskResultRecorder(completionRepo, profileRepo, streakRepo, taskRepo),
            appClock = appClock,
        )
        val allViewModel = TaskViewModel(
            taskRepository = taskRepo,
            taskCompletionRepository = completionRepo,
            difficultySettingsRepository = diffRepo,
            appClock = appClock,
        )
        val historyViewModel = TaskHistoryViewModel(
            taskCompletionRepository = completionRepo,
            taskRepository = taskRepo,
        )
        return Triple(todayViewModel, allViewModel, historyViewModel)
    }

    @Test
    fun createQuest_andTapOnQuestsScreen_completesImmediatelyWithoutRestart() {
        val (taskRepo, completionRepo) = setupTestBench()
        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(taskRepo, completionRepo)

        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.All,
                onNewQuest = {},
                onEditQuest = {},
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }

        // 1. Create a task
        val tomorrow = now.toLocalDate().plusDays(1)
        val createdId = runBlocking {
            taskRepo.insertTask(
                TaskEntity(
                    name = "Tomorrow Quest",
                    description = "",
                    scheduledDay = tomorrow,
                    scheduledTime = LocalTime.of(15, 0),
                    recurrenceType = RecurrenceType.ONE_TIME,
                    category = TaskCategory.LEARNING,
                ),
            )
        }
        composeTestRule.waitForIdle()

        // 2. Tap to complete on QuestsScreen (All tab)
        composeTestRule.onNode(
            isToggleable() and hasAnyAncestor(hasTestTag(QuestsTags.quest(createdId))),
        ).performClick()
        composeTestRule.waitForIdle()

        // 3. Verify it was completed in repository
        val logs = completionRepo.logs.value
        assertEquals("Quest should be completed when tapped on All tab", 1, logs.size)
        assertEquals(createdId, logs.first().taskId)
    }

    @Test
    fun quest_isTappableRightAfterCreation() {
        val (taskRepo, completionRepo) = setupTestBench()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val formViewModel = TaskFormViewModel(taskRepo, TaskAlarmScheduler(context), appClock)
        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(taskRepo, completionRepo)

        var editedId: Long? = null
        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.Today,
                onNewQuest = {},
                onEditQuest = { editedId = it },
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }

        formViewModel.onNameChanged("Fresh Quest")
        formViewModel.onTimeSelected(LocalTime.of(11, 0))
        var created = false
        formViewModel.saveTask { created = true }
        composeTestRule.waitForIdle()
        assertTrue(created)

        val createdTask = taskRepo.tasks.value.first { it.name == "Fresh Quest" }

        // Tap the quest row to edit
        composeTestRule.onNodeWithTag(QuestsTags.quest(createdTask.id)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(createdTask.id, editedId)
    }

    @Test
    fun quest_isTappableAfterRotation() {
        val (taskRepo, completionRepo) = setupTestBench()
        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(taskRepo, completionRepo)

        val createdId = runBlocking {
            taskRepo.insertTask(
                TaskEntity(
                    name = "Rotation Quest",
                    description = "",
                    scheduledDay = now.toLocalDate(),
                    scheduledTime = LocalTime.of(14, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.HEALTH,
                ),
            )
        }

        var rotationKey by mutableStateOf(0)
        var editedId: Long? = null
        // Initial composition
        composeTestRule.setContent {
            key(rotationKey) {
                QuestsScreen(
                    initialTab = QuestTab.Today,
                    onNewQuest = {},
                    onEditQuest = { editedId = it },
                    onOpenHistoryQuest = {},
                    todayViewModel = todayViewModel,
                    allViewModel = allViewModel,
                    historyViewModel = historyViewModel,
                )
            }
        }
        composeTestRule.waitForIdle()

        // Simulate activity/screen rotation by disposing and recomposing with the retained ViewModels
        rotationKey++
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QuestsTags.quest(createdId)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(createdId, editedId)
    }

    @Test
    fun quest_isTappableAfterAppRestart() {
        val (taskRepo, completionRepo) = setupTestBench()

        val createdId = runBlocking {
            taskRepo.insertTask(
                TaskEntity(
                    name = "Restart Quest",
                    description = "",
                    scheduledDay = now.toLocalDate(),
                    scheduledTime = LocalTime.of(16, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.FITNESS,
                ),
            )
        }

        // Simulate app restart: fresh viewmodels created from the persistent repository
        val (restartedTodayVm, restartedAllVm, restartedHistoryVm) = buildQuestsScreen(taskRepo, completionRepo)

        var editedId: Long? = null
        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.Today,
                onNewQuest = {},
                onEditQuest = { editedId = it },
                onOpenHistoryQuest = {},
                todayViewModel = restartedTodayVm,
                allViewModel = restartedAllVm,
                historyViewModel = restartedHistoryVm,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QuestsTags.quest(createdId)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(createdId, editedId)
    }
}
