package com.pixelquest.app.ui.settings

import com.pixelquest.app.testing.closeAfterWrites
import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.data.backup.AutoBackups
import com.pixelquest.app.data.backup.BackupRestorer
import com.pixelquest.app.data.backup.LostProgress
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.domain.LevelUpSignalManager
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.screens.settings.LostProgressPrompt
import com.pixelquest.app.ui.screens.settings.LostProgressViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** The dashboard offers the automatic backup when the game was found empty: a lost database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class LostProgressPromptTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val dir = File(context.filesDir, "lost-prompt-test")
    private lateinit var db: AppDatabase
    private lateinit var viewModel: LostProgressViewModel

    @Before
    fun setUp(): Unit = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dir.deleteRecursively()
        AutoBackups(dir).save(
            LocalDate.of(2026, 10, 11),
            """{"formatVersion": 4, "userProfile": {"id": 1, "username": "Aria", "avatarId": "avatar_mage", "level": 3, "totalXp": 1200}, "tasks": []}"""
        )
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        db.userProfileDao().insertProfile(UserProfileEntity(username = "PixelHero", avatarId = "avatar_hero"))
        viewModel = LostProgressViewModel(LostProgress(BackupRestorer(db, TaskAlarmScheduler(context), LevelUpSignalManager(context)), AutoBackups(dir)))
        Unit
    }

    @After
    fun tearDown() {
        db.closeAfterWrites()
        Dispatchers.resetMain()
        dir.deleteRecursively()
    }

    private fun showPrompt() {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) { LostProgressPrompt(viewModel) }
        }
        composeTestRule.waitUntil(5_000) { viewModel.prompt.value != null }
    }

    @Test
    fun theBackupIsOffered_andRestored() = runBlocking {
        showPrompt()
        composeTestRule.onNodeWithText("from 11 Oct: Aria, level 3", substring = true).assertExists()

        composeTestRule.onNodeWithText("RESTORE 11 OCT").performClick()
        composeTestRule.waitUntil(5_000) { runBlocking { db.userProfileDao().getProfile().first()!!.totalXp } == 1200 }

        assertNull(viewModel.prompt.value)
        assertEquals("Aria", db.userProfileDao().getProfile().first()!!.username)
    }

    @Test
    fun notNow_isRemembered() = runBlocking {
        showPrompt()
        composeTestRule.onNodeWithText("NOT NOW").performClick()
        composeTestRule.waitUntil(5_000) { AutoBackups(dir).declinedThrough() != null }

        assertEquals(LocalDate.of(2026, 10, 11), AutoBackups(dir).declinedThrough())
        assertEquals("the game is left as it is", "PixelHero", db.userProfileDao().getProfile().first()!!.username)
    }
}
