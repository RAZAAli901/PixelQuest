package com.pixelquest.app.ui.settings

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.settings.SettingsViewModel
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REDUCE MOTION can be switched on. The setting was stored and the theme read it, but Settings had
 * no switch for it, and the looping animations (the leaderboard pulse, the AI Coach spinner, the
 * level-up bounce) didn't check it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReduceMotionToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private val settings = FakeSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
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
    fun theSwitch_turnsTheSettingOnAndOff() {
        viewModel.setReduceMotionEnabled(true)
        assertTrue(settings.isReduceMotionEnabled.value)

        viewModel.setReduceMotionEnabled(false)
        assertFalse(settings.isReduceMotionEnabled.value)
    }

    @Test
    fun theTheme_passesItToEveryScreen() {
        var underReduced: Boolean? = null
        var underNormal: Boolean? = null

        composeTestRule.setContent {
            PixelQuestTheme(isReduceMotion = true) { underReduced = LocalReduceMotion.current }
            PixelQuestTheme(isReduceMotion = false) { underNormal = LocalReduceMotion.current }
        }
        composeTestRule.waitForIdle()

        assertEquals(true, underReduced)
        assertEquals(false, underNormal)
    }
}
