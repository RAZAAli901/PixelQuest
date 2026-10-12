package com.pixelquest.app.ui.onboarding

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import com.pixelquest.app.ui.screens.onboarding.OnboardingViewModel
import com.pixelquest.app.ui.screens.onboarding.OnboardingWelcomeScreen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The welcome screen restores a backup instead of starting a new hero. The upgrade path from 1.1.0
 * is export, reinstall, import, and the import had to wait until onboarding had made a hero to replace.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class RestoreFromWelcomeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: AppDatabase
    private val settings = FakeSettingsRepository(onboardingDone = false)
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        viewModel = OnboardingViewModel(FakeUserProfileRepository(), FakeDifficultySettingsRepository(), settings, restorer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun pick(text: String) {
        viewModel.onBackupFileSelected(context, Uri.fromFile(File(context.cacheDir, "picked.json").apply { writeText(text) }))
        repeat(50) { if (viewModel.uiState.value.restoreSummary == null && viewModel.uiState.value.restoreMessage == null) Thread.sleep(20) }
    }

    @Test
    fun aBackupPickedOnTheWelcomeScreen_isRestored_andOnboardingIsDone() = runBlocking {
        pick("""{"formatVersion": 4, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 5, "totalXp": 2400}, "tasks": []}""")
        assertTrue(viewModel.uiState.value.restoreSummary!!.startsWith("Aria, level 5"))

        var finished = false
        viewModel.confirmRestore { finished = true }
        repeat(50) { if (!finished) Thread.sleep(20) }

        assertTrue(finished)
        assertTrue(settings.onboardingComplete.value)
        assertEquals("Aria", db.userProfileDao().getProfile().first()!!.username)
        assertEquals(2400, db.userProfileDao().getProfile().first()!!.totalXp)
    }

    private companion object {
        const val ARIA_LEVEL_3 =
            """{"formatVersion": 4, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 3, "totalXp": 1200}, "tasks": []}"""
    }

    @Test
    fun aFileThatIsntABackup_saysSo() {
        pick("""{"something": "else"}""")
        assertEquals(OnboardingViewModel.NOT_A_BACKUP, viewModel.uiState.value.restoreMessage)
    }

    @Test
    fun theWelcomeScreen_offersIt_whenItCan() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { OnboardingWelcomeScreen(onRestoreClick = {}) }
        }
        composeTestRule.onNodeWithText("📥 I HAVE A BACKUP").assertExists()
    }

    @Test
    fun withoutARestorer_theWelcomeScreenDoesnt() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { OnboardingWelcomeScreen() }
        }
        assertEquals(0, composeTestRule.onAllNodesWithText("📥 I HAVE A BACKUP").fetchSemanticsNodes().size)
    }

    @Test
    fun afterTheDatabaseWasLost_theWelcomeScreenOffersTheAutomaticBackup(): Unit = runBlocking {
        val dir = File(context.filesDir, "welcome-auto").apply { deleteRecursively() }
        val day = java.time.LocalDate.of(2026, 10, 11)
        com.pixelquest.app.data.backup.AutoBackups(dir).save(
            day, """{"formatVersion": 4, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 3, "totalXp": 1200}, "tasks": []}"""
        )
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        val vm = OnboardingViewModel(FakeUserProfileRepository(), FakeDifficultySettingsRepository(), settings, restorer, com.pixelquest.app.data.backup.AutoBackups(dir))

        assertEquals(day, vm.latestAutoBackup)
        vm.onAutoBackupSelected(day)
        assertTrue(vm.uiState.value.restoreSummary!!.startsWith("Aria, level 3"))
        var finished = false
        vm.confirmRestore { finished = true }
        repeat(50) { if (!finished) Thread.sleep(20) }

        assertEquals(1200, db.userProfileDao().getProfile().first()!!.totalXp)
        dir.deleteRecursively()
    }

    @Test
    fun startingANewHeroInstead_isntAskedAgainOnTheDashboard() {
        val dir = File(context.filesDir, "welcome-new-hero").apply { deleteRecursively() }
        val backups = com.pixelquest.app.data.backup.AutoBackups(dir)
        val day = java.time.LocalDate.of(2026, 10, 11)
        backups.save(day, ARIA_LEVEL_3)
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))
        val vm = OnboardingViewModel(FakeUserProfileRepository(), FakeDifficultySettingsRepository(), settings, restorer, backups)
        assertEquals(day, vm.latestAutoBackup)

        var finished = false
        vm.completeOnboarding { finished = true }
        repeat(50) { if (!finished) Thread.sleep(20) }

        assertEquals(day, backups.declinedThrough())
        assertEquals(
            "and after a reset, the welcome screen doesn't offer it",
            null, OnboardingViewModel(FakeUserProfileRepository(), FakeDifficultySettingsRepository(), settings, restorer, backups).latestAutoBackup
        )
        dir.deleteRecursively()
    }

    @Test
    fun aBackupCutShort_orOfAnEmptyGame_isSkippedForAnOlderOne() {
        val dir = File(context.filesDir, "welcome-skip").apply { deleteRecursively() }
        val backups = com.pixelquest.app.data.backup.AutoBackups(dir)
        val oct9 = java.time.LocalDate.of(2026, 10, 9)
        backups.save(oct9, ARIA_LEVEL_3)
        backups.save(oct9.plusDays(1), ARIA_LEVEL_3.replace("\"totalXp\": 1200", "\"totalXp\": 0").replace("\"level\": 3", "\"level\": 1"))
        backups.save(oct9.plusDays(2), ARIA_LEVEL_3.take(40))
        val restorer = BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context))

        val vm = OnboardingViewModel(FakeUserProfileRepository(), FakeDifficultySettingsRepository(), settings, restorer, backups)

        assertEquals(oct9, vm.latestAutoBackup)
        dir.deleteRecursively()
    }

    @Test
    fun theWelcomeScreen_showsTheAutomaticBackupsDay() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { OnboardingWelcomeScreen(onRestoreClick = {}, autoBackupDay = "11 Oct") }
        }
        composeTestRule.onNodeWithText("↺ RESTORE MY GAME FROM 11 OCT").assertExists()
    }
}

