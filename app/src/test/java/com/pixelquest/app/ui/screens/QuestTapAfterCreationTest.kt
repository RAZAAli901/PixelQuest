package com.pixelquest.app.ui.screens

import android.app.Application
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * B02: Failing UI test reproducing that a just-created quest cannot be completed
 * or tapped on QuestsScreen without restarting the app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class QuestTapAfterCreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = LocalDateTime.of(2026, 10, 27, 10, 0) // Tuesday
    private val appClock = FixedClock(now)

    @Test
    fun createQuest_andTapOnQuestsScreen_completesImmediatelyWithoutRestart() {
        val taskRepo = FakeTaskRepository()
        val completionRepo = FakeTaskCompletionRepository()
        val profileRepo = FakeUserProfileRepository()
        val streakRepo = FakeStreakRepository()
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, perfectDayThreshold = 0.7f, daysRequiredPerLevel = 7),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val alarmScheduler = TaskAlarmScheduler(context)

        val formViewModel = TaskFormViewModel(taskRepo, alarmScheduler, appClock)
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

        var editedQuestId: Long? = null
        composeTestRule.setContent {
            QuestsScreen(
                initialTab = QuestTab.All,
                onNewQuest = {},
                onEditQuest = { editedQuestId = it },
                onOpenHistoryQuest = {},
                todayViewModel = todayViewModel,
                allViewModel = allViewModel,
                historyViewModel = historyViewModel,
            )
        }

        // 1. Create a task for tomorrow (visible on All tab)
        val tomorrow = now.toLocalDate().plusDays(1)
        val createdId = kotlinx.coroutines.runBlocking {
            taskRepo.insertTask(
                com.pixelquest.app.data.local.entity.TaskEntity(
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
}
