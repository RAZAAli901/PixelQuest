package com.pixelquest.app.integration

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.TaskEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * RESET ALL PROGRESS from Settings: the ViewModel's reset wipes the database through ProgressReset
 * and sends the player back to onboarding.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ResetProgressIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val settings = FakeSettingsRepository(onboardingDone = true)
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.taskDao().insertTask(
            TaskEntity(
                id = 1, name = "Task 1", description = "Desc", scheduledDay = LocalDate.now(), scheduledTime = LocalTime.of(9, 0),
                recurrenceType = RecurrenceType.DAILY, category = TaskCategory.FITNESS
            )
        )
        db.userProfileDao().insertProfile(UserProfileEntity(username = "OldHero", avatarId = "avatar_mage", level = 5, totalXp = 500))
        db.difficultySettingsDao().insertSettings(DifficultySettingsEntity(difficultyLevel = DifficultyLevel.HARD, perfectDayThreshold = 0.9f))

        val scheduler = TaskAlarmScheduler(context)
        viewModel = SettingsViewModel(
            settings,
            FakeUserProfileRepository(),
            FakeDifficultySettingsRepository(),
            FakeTaskRepository(),
            scheduler,
            ProgressReset(db, scheduler, LevelUpSignalManager(context)),
            BackupRestorer(db, scheduler, LevelUpSignalManager(context))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testPerformFullResetWipesDataAndResetsOnboarding() = runBlocking {
        var callbackFired = false
        viewModel.performFullReset { callbackFired = true }
        // The reset runs on Room's executor; let the ViewModel's coroutine finish.
        repeat(500) {
            if (callbackFired) return@repeat
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(10)
        }

        assertTrue(callbackFired)
        assertFalse(settings.onboardingComplete.value)
        assertTrue(db.taskDao().getAllTasks().first().isEmpty())
        val profile = db.userProfileDao().getProfile().first()!!
        assertEquals("PixelHero", profile.username)
        assertEquals("avatar_hero", profile.avatarId)
        assertEquals(1, profile.level)
        assertEquals(DifficultyLevel.MEDIUM, db.difficultySettingsDao().getCurrentDifficulty().first()!!.difficultyLevel)
    }
}
