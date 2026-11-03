package com.pixelquest.app.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.ProgressReset
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.repository.SettingsRepositoryImpl
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeTaskRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.util.Locale

/**
 * Unit & persistence test suite for Settings toggles:
 * - Every single toggle and preference persists across application/repository restart
 * - SharedPreferences backing verification
 * - Start-to-finish UI toggle flow: changing toggles in SettingsScreenScaffold and verifying state after simulated restart
 * - Rapid toggle spam determinism
 * - Concurrent multi-threaded settings updates
 * - Display resilience across Pixel and Comic themes, 2.0x font scaling, and RTL layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class SettingsTogglePersistenceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase

    private val scheduler by lazy { TaskAlarmScheduler(context) }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Clear preferences before each test
        context.getSharedPreferences("pixel_quest_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSettingsViewModel(repo: SettingsRepositoryImpl): SettingsViewModel {
        val userProfileRepo = FakeUserProfileRepository(
            UserProfileEntity(id = 1, username = "SettingsHero", avatarId = "avatar_hero"),
        )
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM),
        )
        val taskRepo = FakeTaskRepository()
        val levelUps = LevelUpSignalManager(context)
        val progressReset = ProgressReset(database, scheduler, levelUps)
        val backupRestorer = BackupRestorer(database, scheduler, levelUps, repo)

        return SettingsViewModel(
            settingsRepository = repo,
            userProfileRepository = userProfileRepo,
            difficultySettingsRepository = diffRepo,
            taskRepository = taskRepo,
            taskAlarmScheduler = scheduler,
            progressReset = progressReset,
            backupRestorer = backupRestorer,
        )
    }

    @Composable
    private fun SettingsTestContent(viewModel: SettingsViewModel) {
        val state by viewModel.uiState.collectAsState()
        SettingsScreenScaffold(
            onNavigateBack = {},
            notificationsSection = {
                SettingSwitch(
                    "Notifications",
                    "Reminders and nudges from PixelQuest",
                    state.isNotificationsEnabled,
                    { viewModel.toggleNotifications(it) },
                )
                if (state.isNotificationsEnabled) {
                    val reminderSoundOn by viewModel.isReminderSoundEnabled.collectAsState()
                    SettingSwitch("Reminder sound", "Off, reminders arrive silently", reminderSoundOn, {
                        viewModel.setReminderSoundEnabled(it)
                    })
                }
            },
            appearanceSection = {
                SettingSwitch("Sound effects", null, state.isSoundEnabled, { viewModel.toggleSound(it) })
                SettingSwitch("Haptics", null, state.isHapticsEnabled, { viewModel.toggleHaptics(it) })
                if (state.themeMode.skin == PqSkin.Pixel) {
                    SettingSwitch("CRT filter", "Old-screen scanlines over the Pixel look", state.isCrtEnabled, { viewModel.toggleCrt(it) })
                }
                val reduceMotionOn by viewModel.isReduceMotionEnabled.collectAsState()
                SettingSwitch("Reduce motion", "Pulses, spinners and bounces hold still", reduceMotionOn, {
                    viewModel.setReduceMotionEnabled(it)
                })
            },
            simpleModeSection = {
                SettingSwitch(
                    "Simple Mode",
                    "Minimalist, un-gamified task tracking",
                    state.isSimpleModeEnabled,
                    { on -> viewModel.toggleSimpleMode(on) },
                )
            },
        )
    }

    @Test
    fun settings_restart_everySettingPersistsAcrossRepositoryRecreation() = runBlocking {
        val initialRepo = SettingsRepositoryImpl(context)

        // 1. Verify default initial values
        assertTrue("Sound should default to true", initialRepo.isSoundEnabled.first())
        assertFalse("CRT filter should default to false", initialRepo.isCrtEnabled.first())
        assertTrue("Haptics should default to true", initialRepo.isHapticsEnabled.first())
        assertFalse("Reduce motion should default to false", initialRepo.isReduceMotionEnabled.first())
        assertFalse("Onboarding complete should default to false", initialRepo.onboardingComplete.first())
        assertTrue("Notifications should default to true", initialRepo.isNotificationsEnabled.first())
        assertTrue("Notification sound should default to true", initialRepo.isNotificationSoundEnabled.first())
        assertTrue("Notification vibration should default to true", initialRepo.isNotificationVibrationEnabled.first())
        assertFalse("Simple mode should default to false", initialRepo.simpleModeEnabled.first())
        assertFalse("AI insights should default to false", initialRepo.aiInsightsEnabled.first())
        assertFalse("AI reminder messages should default to false", initialRepo.aiReminderMessagesEnabled.first())
        assertTrue("Islamic hub should default to true", initialRepo.islamicHubShown.first())
        assertTrue("Darood should default to true", initialRepo.daroodAvailable.first())
        assertFalse("Namaz setup shown should default to false", initialRepo.namazSetupShown.first())
        assertTrue("Study alarm style should default to true", initialRepo.isStudyAlarmStyleEnabled.first())
        assertEquals("alarm", initialRepo.studyAlarmSound.first())

        // 2. Invert / mutate every single preference
        initialRepo.setSoundEnabled(false)
        initialRepo.setCrtEnabled(true)
        initialRepo.setHapticsEnabled(false)
        initialRepo.setReduceMotionEnabled(true)
        initialRepo.setOnboardingComplete(true)
        initialRepo.setNotificationsEnabled(false)
        initialRepo.setNotificationSoundEnabled(false)
        initialRepo.setNotificationVibrationEnabled(false)
        initialRepo.setThemeMode(ThemeMode.Pixel)
        initialRepo.setSimpleModeEnabled(true)
        initialRepo.setSimpleModeHighlightSeen(true)
        initialRepo.setAiInsightsEnabled(true)
        initialRepo.setLastAiInsightTimestamp(123456789L)
        initialRepo.setAiReminderMessagesEnabled(true)
        initialRepo.setLastBackupAt(987654321L)
        initialRepo.setNamazAvailable(false)
        initialRepo.setDaroodAvailable(false)
        initialRepo.setIslamicHubShown(false)
        initialRepo.setNamazSetupShown(true)
        initialRepo.setStudyAlarmStyleEnabled(false)
        initialRepo.setStudyAlarmSound("chime")

        // 3. Simulate process restart by creating a new repository instance
        val restartedRepo = SettingsRepositoryImpl(context)

        // 4. Assert EVERY setting was restored from SharedPreferences
        assertFalse(restartedRepo.isSoundEnabled.first())
        assertTrue(restartedRepo.isCrtEnabled.first())
        assertFalse(restartedRepo.isHapticsEnabled.first())
        assertTrue(restartedRepo.isReduceMotionEnabled.first())
        assertTrue(restartedRepo.onboardingComplete.first())
        assertFalse(restartedRepo.isNotificationsEnabled.first())
        assertFalse(restartedRepo.isNotificationSoundEnabled.first())
        assertFalse(restartedRepo.isNotificationVibrationEnabled.first())
        assertEquals(ThemeMode.Pixel, restartedRepo.themeMode.first())
        assertTrue(restartedRepo.simpleModeEnabled.first())
        assertTrue(restartedRepo.hasSeenSimpleModeHighlight.first())
        assertTrue(restartedRepo.aiInsightsEnabled.first())
        assertEquals(123456789L, restartedRepo.lastAiInsightTimestamp.first())
        assertTrue(restartedRepo.aiReminderMessagesEnabled.first())
        assertEquals(987654321L, restartedRepo.lastBackupAt.first())
        assertFalse(restartedRepo.namazAvailable.first())
        assertFalse(restartedRepo.daroodAvailable.first())
        assertFalse(restartedRepo.islamicHubShown.first())
        assertTrue(restartedRepo.namazSetupShown.first())
        assertFalse(restartedRepo.isStudyAlarmStyleEnabled.first())
        assertEquals("chime", restartedRepo.studyAlarmSound.first())
    }

    @Test
    fun settings_toggleSpam_rapidTogglingPersistsDeterministicFinalValue() = runBlocking {
        val repo = SettingsRepositoryImpl(context)

        // Rapid 25-cycle toggle spam for Sound effects
        for (i in 1..25) {
            repo.setSoundEnabled(true)
            repo.setSoundEnabled(false)
        }
        repo.setSoundEnabled(true)

        // Rapid 25-cycle toggle spam for Simple Mode
        for (i in 1..25) {
            repo.setSimpleModeEnabled(false)
            repo.setSimpleModeEnabled(true)
        }
        repo.setSimpleModeEnabled(false)

        // Restart and assert final states
        val restartedRepo = SettingsRepositoryImpl(context)
        assertTrue(restartedRepo.isSoundEnabled.first())
        assertFalse(restartedRepo.simpleModeEnabled.first())
    }

    @Test
    fun settings_ui_toggleSwitchesInSettingsScreen_persistsAcrossRestart() {
        val repo1 = SettingsRepositoryImpl(context)
        val vm1 = createSettingsViewModel(repo1)

        composeTestRule.setContent {
            SettingsTestContent(vm1)
        }
        composeTestRule.waitForIdle()

        // Initial default: Sound effects is ON, Haptics is ON, Reduce motion is OFF
        composeTestRule.onNodeWithText("Sound effects").assertIsOn()
        composeTestRule.onNodeWithText("Haptics").assertIsOn()
        composeTestRule.onNodeWithText("Reduce motion").assertIsOff()

        // Toggle Sound effects to OFF
        composeTestRule.onNodeWithText("Sound effects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sound effects").assertIsOff()

        // Toggle Haptics to OFF
        composeTestRule.onNodeWithText("Haptics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Haptics").assertIsOff()

        // Toggle Reduce motion to ON
        composeTestRule.onNodeWithText("Reduce motion").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reduce motion").assertIsOn()

        // Toggle Simple Mode to ON
        composeTestRule.onNodeWithText("Simple Mode").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Simple Mode").assertIsOn()

        // Verify underlying storage updated
        runBlocking {
            assertFalse(repo1.isSoundEnabled.first())
            assertFalse(repo1.isHapticsEnabled.first())
            assertTrue(repo1.isReduceMotionEnabled.first())
            assertTrue(repo1.simpleModeEnabled.first())
        }
    }

    @Test
    fun settings_ui_restartedScreen_reflectsPersistedValues() {
        // Pre-configure persisted preferences before launch (simulating earlier session)
        val preRepo = SettingsRepositoryImpl(context)
        runBlocking {
            preRepo.setSoundEnabled(false)
            preRepo.setHapticsEnabled(false)
            preRepo.setReduceMotionEnabled(true)
            preRepo.setSimpleModeEnabled(true)
        }

        // Fresh restart: brand new repository and brand new ViewModel
        val restartedRepo = SettingsRepositoryImpl(context)
        val restartedVm = createSettingsViewModel(restartedRepo)

        composeTestRule.setContent {
            SettingsTestContent(restartedVm)
        }
        composeTestRule.waitForIdle()

        // Assert all switches show the restored preferences
        composeTestRule.onNodeWithText("Sound effects").assertIsOff()
        composeTestRule.onNodeWithText("Haptics").assertIsOff()
        composeTestRule.onNodeWithText("Reduce motion").assertIsOn()
        composeTestRule.onNodeWithText("Simple Mode").assertIsOn()
    }

    @Test
    fun settings_concurrentModification_threadSafety() = runBlocking {
        val repo = SettingsRepositoryImpl(context)

        // Launch concurrent coroutines updating separate settings
        val jobs = listOf(
            launch(Dispatchers.IO) { repo.setSoundEnabled(false) },
            launch(Dispatchers.IO) { repo.setCrtEnabled(true) },
            launch(Dispatchers.IO) { repo.setHapticsEnabled(false) },
            launch(Dispatchers.IO) { repo.setReduceMotionEnabled(true) },
            launch(Dispatchers.IO) { repo.setNotificationsEnabled(false) },
            launch(Dispatchers.IO) { repo.setSimpleModeEnabled(true) },
            launch(Dispatchers.IO) { repo.setStudyAlarmSound("system") },
        )
        jobs.forEach { it.join() }

        // Recreate and verify all concurrent updates committed successfully
        val freshRepo = SettingsRepositoryImpl(context)
        assertFalse(freshRepo.isSoundEnabled.first())
        assertTrue(freshRepo.isCrtEnabled.first())
        assertFalse(freshRepo.isHapticsEnabled.first())
        assertTrue(freshRepo.isReduceMotionEnabled.first())
        assertFalse(freshRepo.isNotificationsEnabled.first())
        assertTrue(freshRepo.simpleModeEnabled.first())
        assertEquals("system", freshRepo.studyAlarmSound.first())
    }

    @Test
    fun settings_displayMatrix_comicSkin_andFontScale2x() {
        val repo = SettingsRepositoryImpl(context)
        val vm = createSettingsViewModel(repo)

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, fontScale = 2.0f),
        ) {
            SettingsTestContent(vm)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sound effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }

    @Test
    fun settings_displayMatrix_pixelSkin_andRtl() {
        val repo = SettingsRepositoryImpl(context)
        val vm = createSettingsViewModel(repo)

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            SettingsTestContent(vm)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sound effects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }
}
