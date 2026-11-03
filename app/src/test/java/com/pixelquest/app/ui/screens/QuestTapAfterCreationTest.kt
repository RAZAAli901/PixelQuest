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
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.StreakEntity
import com.pixelquest.app.data.local.entity.TaskCompletionLogEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
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
import com.pixelquest.app.ui.components.QuestTags
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
 *
 * V027: Quest gestures: tap, hold, icon, circle, no double firing,
 * and immediate interactivity right after creation.
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
        profileRepo: FakeUserProfileRepository = FakeUserProfileRepository(),
        streakRepo: FakeStreakRepository = FakeStreakRepository(),
    ): Triple<TodayViewModel, TaskViewModel, TaskHistoryViewModel> {
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

    @Test
    fun quest_allGesturesRightAfterCreation_tapHoldIconCircle() {
        val (taskRepo, completionRepo) = setupTestBench()
        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(taskRepo, completionRepo)

        var editedId: Long? = null
        var editCount = 0

        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.Today,
                onNewQuest = {},
                onEditQuest = {
                    editedId = it
                    editCount++
                },
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }

        // Insert quest into repository
        val createdId = runBlocking {
            taskRepo.insertTask(
                TaskEntity(
                    name = "Gesture Quest",
                    description = "Testing all gestures",
                    scheduledDay = now.toLocalDate(),
                    scheduledTime = LocalTime.of(12, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.CHORES,
                ),
            )
        }
        composeTestRule.waitForIdle()

        // 1. Icon gesture: tapping icon triggers onEditQuest
        composeTestRule.onNodeWithTag(QuestTags.icon(createdId)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(createdId, editedId)
        assertEquals(1, editCount)

        // 2. Hold gesture: long pressing card body triggers onEditQuest
        editedId = null
        composeTestRule.onNodeWithTag(QuestTags.body(createdId)).performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        assertEquals(createdId, editedId)
        assertEquals(2, editCount)

        // 3. Tap gesture: short click on card body triggers onEditQuest
        editedId = null
        composeTestRule.onNodeWithTag(QuestTags.body(createdId)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(createdId, editedId)
        assertEquals(3, editCount)

        // 4. Circle gesture: tapping completion circle completes the quest
        composeTestRule.onNodeWithTag(QuestTags.complete(createdId)).performClick()
        composeTestRule.waitForIdle()

        val logs = completionRepo.logs.value
        assertEquals("Quest should be completed via circle tap", 1, logs.size)
        assertEquals(createdId, logs.first().taskId)
        assertTrue(logs.first().wasCompleted)
    }

    @Test
    fun quest_circleRapidDoubleTap_preventsDoubleFiringAndDuplicateXp() {
        val (taskRepo, completionRepo) = setupTestBench()
        val profileRepo = FakeUserProfileRepository()
        runBlocking {
            profileRepo.updateProfile(
                UserProfileEntity(
                    id = 1,
                    username = "Hero",
                    avatarId = "avatar_knight",
                    totalXp = 50,
                    level = 1,

                ),
            )
        }
        val streakRepo = FakeStreakRepository()
        runBlocking {
            streakRepo.updateStreak(
                StreakEntity(
                    currentStreak = 5,
                    longestStreak = 10,
                    lastCompletedDate = now.toLocalDate().minusDays(1),
                ),
            )
        }

        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(
            taskRepo = taskRepo,
            completionRepo = completionRepo,
            profileRepo = profileRepo,
            streakRepo = streakRepo,
        )

        // Create a task that has a prior completion so that this completion awards XP (> 0)
        val createdId = runBlocking {
            val id = taskRepo.insertTask(
                TaskEntity(
                    name = "Recurring Quest",
                    description = "",
                    scheduledDay = now.toLocalDate(),
                    scheduledTime = LocalTime.of(9, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.FITNESS,
                ),
            )
            // Seed a prior completion on an earlier date
            completionRepo.insertLog(
                TaskCompletionLogEntity(
                    taskId = id,
                    completedDate = now.toLocalDate().minusDays(1),
                    pointsAwarded = 25,
                    wasCompleted = true,
                ),
            )
            id
        }

        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.Today,
                onNewQuest = {},
                onEditQuest = {},
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }
        composeTestRule.waitForIdle()

        // Rapid double tap on completion circle
        composeTestRule.onNodeWithTag(QuestTags.complete(createdId)).performClick()
        composeTestRule.onNodeWithTag(QuestTags.complete(createdId)).performClick()
        composeTestRule.waitForIdle()

        // Verify only 1 completion log for today
        val todayLogs = completionRepo.logs.value.filter {
            it.taskId == createdId && it.completedDate == now.toLocalDate()
        }
        assertEquals("Rapid double tap must record exactly 1 completion log for today", 1, todayLogs.size)
        assertTrue(todayLogs.first().wasCompleted)

        // Verify XP was awarded only once (pointsAwarded = 25 for streak 5)
        val finalProfile = profileRepo.profile.value
        val expectedXp = 50 + todayLogs.first().pointsAwarded
        assertEquals("XP should be awarded only once despite double tap", expectedXp, finalProfile?.totalXp)
    }

    @Test
    fun quest_alreadyCompletedCircleTap_isIgnored() {
        val (taskRepo, completionRepo) = setupTestBench()
        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(taskRepo, completionRepo)

        val createdId = runBlocking {
            val id = taskRepo.insertTask(
                TaskEntity(
                    name = "Done Quest",
                    description = "",
                    scheduledDay = now.toLocalDate(),
                    scheduledTime = LocalTime.of(8, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.OTHER,
                ),
            )
            completionRepo.insertLog(
                TaskCompletionLogEntity(
                    taskId = id,
                    completedDate = now.toLocalDate(),
                    pointsAwarded = 20,
                    wasCompleted = true,
                ),
            )
            id
        }

        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.Today,
                onNewQuest = {},
                onEditQuest = {},
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }
        composeTestRule.waitForIdle()

        // Tap complete circle on already-completed quest
        composeTestRule.onNodeWithTag(QuestTags.complete(createdId)).performClick()
        composeTestRule.waitForIdle()

        // Log count should remain 1
        assertEquals(1, completionRepo.logs.value.size)
    }

    @Test
    fun quest_cardBodyRapidDoubleTap_invokesEditSafely() {
        val (taskRepo, completionRepo) = setupTestBench()
        val (todayViewModel, allViewModel, historyViewModel) = buildQuestsScreen(taskRepo, completionRepo)

        val createdId = runBlocking {
            taskRepo.insertTask(
                TaskEntity(
                    name = "Tap Quest",
                    description = "",
                    scheduledDay = now.toLocalDate(),
                    scheduledTime = LocalTime.of(10, 0),
                    recurrenceType = RecurrenceType.DAILY,
                    category = TaskCategory.HEALTH,
                ),
            )
        }

        var editCount = 0
        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.Today,
                onNewQuest = {},
                onEditQuest = { editCount++ },
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }
        composeTestRule.waitForIdle()

        // Rapid double tap on card body
        composeTestRule.onNodeWithTag(QuestTags.body(createdId)).performClick()
        composeTestRule.onNodeWithTag(QuestTags.body(createdId)).performClick()
        composeTestRule.waitForIdle()

        assertTrue("Double tap on body should safely trigger edit callback without errors", editCount >= 1)
    }

    @Test
    fun quest_createdViaFormFlow_allZonesInteractiveImmediately() {
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

        formViewModel.onNameChanged("Form Created Quest")
        formViewModel.onTimeSelected(LocalTime.of(13, 0))
        var saved = false
        formViewModel.saveTask { saved = true }
        composeTestRule.waitForIdle()
        assertTrue(saved)

        val newTask = taskRepo.tasks.value.first { it.name == "Form Created Quest" }

        // Test icon tap immediately
        composeTestRule.onNodeWithTag(QuestTags.icon(newTask.id)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(newTask.id, editedId)

        // Test body tap immediately
        editedId = null
        composeTestRule.onNodeWithTag(QuestTags.body(newTask.id)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(newTask.id, editedId)

        // Test complete circle tap immediately
        composeTestRule.onNodeWithTag(QuestTags.complete(newTask.id)).performClick()
        composeTestRule.waitForIdle()
        assertTrue(completionRepo.logs.value.any { it.taskId == newTask.id && it.wasCompleted })
    }
}
