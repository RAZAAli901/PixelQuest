# PixelQuest Test Writing Guide

This guide provides standard, idiomatic worked examples for testing every layer of the PixelQuest architecture:
1. **Use Case (Domain Layer)**
2. **ViewModel (Presentation Layer)**
3. **Repository (Data Layer)**
4. **Compose Screen (UI Layer)**
5. **Worker (WorkManager Background Layer)**

All examples follow the zero-flakiness policy: injected `Clock`, `TestDispatcherProvider`, in-memory fakes, and virtual time control.

---

## 1. Use Case (Domain Layer)

### Principles:
- Pure Kotlin, local JVM execution via `runTest`.
- Inject `TestDispatcherProvider` and `FakeClock`.
- Use in-memory repository fakes (`FakeTaskRepository`, `FakeUserProfileRepository`).
- Deterministic assertions on business rules without delays or mocks.

### Worked Example: `CompleteTaskUseCaseTest.kt`

```kotlin
package com.pixelquest.app.domain.task

import com.pixelquest.app.domain.model.Task
import com.pixelquest.app.testing.FakeClock
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.TestDataGenerator
import com.pixelquest.app.testing.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class CompleteTaskUseCaseTest {

    private val fakeClock = FakeClock(Instant.parse("2026-11-03T10:00:00Z"))
    private val dispatchers = TestDispatcherProvider.create()
    private val taskRepository = FakeTaskRepository()
    private val userProfileRepository = FakeUserProfileRepository()

    private lateinit var useCase: CompleteTaskUseCase

    @Before
    fun setUp() {
        useCase = CompleteTaskUseCase(
            taskRepository = taskRepository,
            userProfileRepository = userProfileRepository,
            clock = fakeClock.asAppClock(),
            dispatchers = dispatchers,
        )
    }

    @Test
    fun completeTask_updatesTaskState_andAwardsXp() = runTest {
        // Arrange
        val user = TestDataGenerator.userProfile(userId = "user_1", totalXp = 100, level = 2)
        userProfileRepository.saveProfile(user)

        val task = TestDataGenerator.task(
            id = "task_1",
            userId = "user_1",
            isCompleted = false,
            xpReward = 50,
        )
        taskRepository.insertTask(task)

        // Act
        val result = useCase(taskId = "task_1")

        // Assert
        assertTrue("Use case execution should succeed", result.isSuccess)
        val updatedTask = taskRepository.getTaskById("task_1")
        assertTrue("Task must be marked completed", updatedTask?.isCompleted == true)
        assertEquals("Completed timestamp must match injected clock", fakeClock.instant(), updatedTask?.completedAt)

        val updatedProfile = userProfileRepository.getProfile("user_1")
        assertEquals("XP must be incremented by task reward", 150, updatedProfile?.totalXp)
    }
}
```

---

## 2. ViewModel (Presentation Layer)

### Principles:
- Uses `@get:Rule val mainDispatcherRule = MainDispatcherRule()`.
- Collects and asserts `StateFlow` emissions deterministically using `app.cash.turbine.test`.
- Triggers UI intents / actions and verifies state transitions and side effects.

### Worked Example: `QuestDetailViewModelTest.kt`

```kotlin
package com.pixelquest.app.ui.quest

import app.cash.turbine.test
import com.pixelquest.app.testing.FakeClock
import com.pixelquest.app.testing.FakeQuestRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.TestDataGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class QuestDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeClock = FakeClock(Instant.parse("2026-11-03T12:00:00Z"))
    private val questRepository = FakeQuestRepository()

    @Test
    fun loadQuest_emitsLoadingThenSuccessState() = runTest {
        // Arrange
        val quest = TestDataGenerator.quest(id = "quest_42", title = "Defeat the Glitch Dragon")
        questRepository.insertQuest(quest)

        val viewModel = QuestDetailViewModel(
            questId = "quest_42",
            questRepository = questRepository,
            clock = fakeClock.asAppClock(),
        )

        // Act & Assert via Turbine
        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertTrue("Initial state is loading", initial.isLoading)

            // Loaded state
            val loaded = awaitItem()
            assertFalse("Loaded state is not loading", loaded.isLoading)
            assertEquals("Defeat the Glitch Dragon", loaded.quest?.title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onAbandonClicked_updatesUiStateToAbandoned() = runTest {
        // Arrange
        val quest = TestDataGenerator.quest(id = "quest_42", isCompleted = false)
        questRepository.insertQuest(quest)

        val viewModel = QuestDetailViewModel(
            questId = "quest_42",
            questRepository = questRepository,
            clock = fakeClock.asAppClock(),
        )

        viewModel.uiState.test {
            skipItems(2) // Skip initial and loaded

            // Act
            viewModel.onAbandonClicked()

            // Assert
            val state = awaitItem()
            assertTrue("Quest should be marked abandoned", state.isAbandoned)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

---

## 3. Repository (Data Layer)

### Principles:
- Integration test running against an in-memory Room database.
- Uses `FakeSupabaseBackend` or mock HTTP client to verify remote synchronization.
- Verifies offline-first caching, local database transactions, and data mappers.

### Worked Example: `TaskRepositoryImplTest.kt`

```kotlin
package com.pixelquest.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.remote.FakeSupabaseBackend
import com.pixelquest.app.domain.model.Task
import com.pixelquest.app.testing.TestDataGenerator
import com.pixelquest.app.testing.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskRepositoryImplTest {

    private lateinit var database: AppDatabase
    private val supabaseBackend = FakeSupabaseBackend()
    private val dispatchers = TestDispatcherProvider.create()
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = TaskRepositoryImpl(
            taskDao = database.taskDao(),
            remoteBackend = supabaseBackend,
            dispatchers = dispatchers,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeTasks_emitsCachedTasksFromLocalDatabase() = runTest {
        // Arrange
        val task = TestDataGenerator.task(id = "task_local_1", title = "Write Kotlin Tests")
        repository.insertTask(task)

        // Act
        val tasks = repository.observeTasks(userId = task.userId).first()

        // Assert
        assertEquals(1, tasks.size)
        assertEquals("Write Kotlin Tests", tasks.first().title)
    }

    @Test
    fun syncWithRemote_insertsMissingRemoteTasksLocally() = runTest {
        // Arrange
        val userId = "user_100"
        supabaseBackend.insertRow(
            table = "tasks",
            userId = userId,
            data = mapOf("id" to "remote_1", "user_id" to userId, "title" to "Sync Quest", "xp_reward" to 100),
        )

        // Act
        repository.sync(userId = userId)

        // Assert
        val localTask = database.taskDao().getById("remote_1")
        assertNotNull("Remote task must be synced into local Room database", localTask)
        assertEquals("Sync Quest", localTask?.title)
    }
}
```

---

## 4. Screen (UI Layer with Jetpack Compose)

### Principles:
- Uses `createComposeRule()` (or `createAndroidComposeRule<ComponentActivity>()`).
- Applies `ThemedTestContent` helper to test under different themes (Pixel, Comic, light, dark, Simple Mode).
- Uses Compose semantics (`onNodeWithTag`, `onNodeWithText`, `performClick`) and checks visibility and content descriptions.

### Worked Example: `QuestCardScreenTest.kt`

```kotlin
package com.pixelquest.app.ui.quest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.testing.ComposeTestHelpers.ThemedTestContent
import com.pixelquest.app.testing.TestDataGenerator
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestCardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun questCard_displaysTitleAndTriggersCallbackOnClick() {
        var clicked = false
        val quest = TestDataGenerator.quest(title = "Slay the Bug", xpReward = 100)

        composeTestRule.setContent {
            ThemedTestContent(theme = ThemeMode.PIXEL, isDark = true) {
                QuestCard(
                    quest = quest,
                    onClick = { clicked = true },
                )
            }
        }

        // Verify title rendered
        composeTestRule.onNodeWithText("Slay the Bug").assertIsDisplayed()

        // Verify click action
        composeTestRule.onNodeWithTag("quest_card_${quest.id}").performClick()
        assertTrue("Click handler must be invoked", clicked)
    }

    @Test
    fun questCard_rendersCorrectlyInSimpleMode() {
        val quest = TestDataGenerator.quest(title = "Simple Quest")

        composeTestRule.setContent {
            ThemedTestContent(theme = ThemeMode.COMIC, isSimpleMode = true) {
                QuestCard(
                    quest = quest,
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Simple Quest").assertIsDisplayed()
    }
}
```

---

## 5. Worker (WorkManager Background Layer)

### Principles:
- Uses `androidx.work.testing.TestListenableWorkerBuilder`.
- Injects fake managers (`FakeNotificationManager`, `FakeAlarmScheduler`, `FakeClock`).
- Verifies return status (`ListenableWorker.Result.success()`) and inspects side effects recorded by fakes.

### Worked Example: `DailyStreakReminderWorkerTest.kt`

```kotlin
package com.pixelquest.app.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.pixelquest.app.testing.FakeClock
import com.pixelquest.app.testing.FakeNotificationManager
import com.pixelquest.app.testing.FakeStreakRepository
import com.pixelquest.app.testing.TestDataGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyStreakReminderWorkerTest {

    private lateinit var context: Context
    private val fakeClock = FakeClock(Instant.parse("2026-11-03T20:00:00Z"))
    private val streakRepository = FakeStreakRepository()
    private val notificationManager = FakeNotificationManager()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun doWork_whenStreakAtRisk_postsWarningNotification() = runTest {
        // Arrange
        val streak = TestDataGenerator.streak(userId = "user_1", currentStreak = 14, completedToday = false)
        streakRepository.saveStreak(streak)

        val worker = TestListenableWorkerBuilder<DailyStreakReminderWorker>(context)
            .build()
            .apply {
                this.streakRepository = this@DailyStreakReminderWorkerTest.streakRepository
                this.notificationManager = this@DailyStreakReminderWorkerTest.notificationManager
                this.clock = fakeClock.asAppClock()
            }

        // Act
        val result = worker.doWork()

        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("One reminder notification must be posted", 1, notificationManager.postedNotifications.size)
        val posted = notificationManager.postedNotifications.first()
        assertTrue("Notification should warn about streak", posted.content.contains("14-day streak"))
    }
}
```

