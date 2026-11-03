package com.pixelquest.app.ui.screens.difficulty

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.data.local.entity.DifficultySettingsEntity
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeDifficultySettingsRepository
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Unit & user flow test suite for Difficulty Selection and Simple Mode toggles:
 * - Start-to-finish user flow: selecting a new difficulty, warning dialog confirmation, database update
 * - Warning dialog dismissal preserving initial level
 * - No-op when tapping already selected level
 * - Toggle spam resilience and rapid selection
 * - Simple Mode enforcement: hard lock preventing difficulty change
 * - Simple Mode toggle persistence and toggling back
 * - Concurrent sync / task completion resilience during setting toggles
 * - Display resilience across Pixel and Comic themes, 2.0x font scaling, and RTL layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class DifficultyAndSimpleModeTogglesFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun difficultySelection_flow_selectNewLevel_showsWarningDialog_confirmUpdatesRepository() {
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = false)
        val viewModel = DifficultyViewModel(
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
        )

        var backClicked = false
        composeTestRule.setContent {
            DifficultySelectionScreen(
                viewModel = viewModel,
                onBackClick = { backClicked = true },
            )
        }
        composeTestRule.waitForIdle()

        // 1. Initial screen assertions
        composeTestRule.onNodeWithText("DIFFICULTY").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIUM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfect day: 70% done").assertIsDisplayed()

        // 2. Tap "HARD"
        composeTestRule.onNodeWithText("HARD").performClick()
        composeTestRule.waitForIdle()

        // 3. Verify confirmation dialog appears with exact thresholds
        composeTestRule.onNodeWithText("Change difficulty?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Changing difficulty will update your target to 90% and level threshold to 14 days",
            substring = true,
        ).assertExists()
        composeTestRule.onNodeWithText("CHANGE").assertIsDisplayed()
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()

        // 4. Tap "CHANGE" to confirm
        composeTestRule.onNodeWithText("CHANGE").performClick()
        composeTestRule.waitForIdle()

        // 5. Verify repository was updated to HARD
        runBlocking {
            val updated = diffRepo.getCurrentDifficulty().first()
            assertEquals(DifficultyLevel.HARD, updated?.difficultyLevel)
            assertEquals(0.90f, updated?.perfectDayThreshold ?: 0f, 0.001f)
            assertEquals(14, updated?.daysRequiredPerLevel)
        }

        // 6. Test Back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue("Back button should invoke onBackClick callback", backClicked)
    }

    @Test
    fun difficultySelection_dismissDialog_preservesOriginalDifficulty() {
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = false)
        val viewModel = DifficultyViewModel(
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
        )

        composeTestRule.setContent {
            DifficultySelectionScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Tap "EASY"
        composeTestRule.onNodeWithText("EASY").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Change difficulty?").assertIsDisplayed()

        // Tap "CANCEL"
        composeTestRule.onNodeWithText("CANCEL").performClick()
        composeTestRule.waitForIdle()

        // Dialog dismissed, repository untouched
        assertEquals(0, composeTestRule.onAllNodesWithText("Change difficulty?").fetchSemanticsNodes().size)
        runBlocking {
            val current = diffRepo.getCurrentDifficulty().first()
            assertEquals(DifficultyLevel.MEDIUM, current?.difficultyLevel)
        }
    }

    @Test
    fun difficultySelection_tappingSameLevel_doesNotTriggerDialog() {
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = false)
        val viewModel = DifficultyViewModel(
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
        )

        composeTestRule.setContent {
            DifficultySelectionScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Tap "MEDIUM" (already selected)
        composeTestRule.onNodeWithText("MEDIUM").performClick()
        composeTestRule.waitForIdle()

        // No dialog triggered
        assertEquals(0, composeTestRule.onAllNodesWithText("Change difficulty?").fetchSemanticsNodes().size)
        assertFalse(viewModel.uiState.value.showWarningDialog)
        assertNull(viewModel.uiState.value.pendingLevel)
    }

    @Test
    fun difficultySelection_toggleSpam_rapidTappingRemainsConsistent() {
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = false)
        val viewModel = DifficultyViewModel(
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
        )

        composeTestRule.setContent {
            DifficultySelectionScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Rapid click sequences: Hard -> CANCEL -> Easy -> CANCEL -> Hardest -> CHANGE
        composeTestRule.onNodeWithText("HARD").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CANCEL").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("EASY").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CANCEL").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("HARDEST").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("CHANGE").performClick()
        composeTestRule.waitForIdle()

        runBlocking {
            val finalDiff = diffRepo.getCurrentDifficulty().first()
            assertEquals(DifficultyLevel.HARDEST, finalDiff?.difficultyLevel)
            assertEquals(30, finalDiff?.daysRequiredPerLevel)
        }
    }

    @Test
    fun difficultySelection_simpleModeLock_preventsAnyDifficultyChange() {
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.MEDIUM, daysRequiredPerLevel = 7),
        )
        val settingsRepo = FakeSettingsRepository(simpleMode = true)
        val viewModel = DifficultyViewModel(
            difficultySettingsRepository = diffRepo,
            settingsRepository = settingsRepo,
        )

        composeTestRule.setContent {
            DifficultySelectionScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // In Simple Mode, tapping any difficulty level should be completely ignored
        composeTestRule.onNodeWithText("EASY").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithText("Change difficulty?").fetchSemanticsNodes().size)

        composeTestRule.onNodeWithText("HARD").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithText("Change difficulty?").fetchSemanticsNodes().size)

        composeTestRule.onNodeWithText("HARDEST").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithText("Change difficulty?").fetchSemanticsNodes().size)

        runBlocking {
            assertEquals(DifficultyLevel.MEDIUM, diffRepo.getCurrentDifficulty().first()?.difficultyLevel)
        }
    }

    @Test
    fun simpleModeToggle_toggleOnAndOff_emitsImmediateStateChanges() = runBlocking {
        val settingsRepo = FakeSettingsRepository(simpleMode = false)
        assertFalse(settingsRepo.simpleModeEnabled.first())

        // Toggle ON
        settingsRepo.setSimpleModeEnabled(true)
        assertTrue(settingsRepo.simpleModeEnabled.first())

        // Toggle OFF
        settingsRepo.setSimpleModeEnabled(false)
        assertFalse(settingsRepo.simpleModeEnabled.first())
    }

    @Test
    fun simpleModeToggle_toggleSpam_persistsFinalStateDeterministically() = runBlocking {
        val settingsRepo = FakeSettingsRepository(simpleMode = false)

        // Rapid 10-cycle toggle spam
        for (i in 1..10) {
            settingsRepo.setSimpleModeEnabled(true)
            assertTrue(settingsRepo.simpleModeEnabled.first())
            settingsRepo.setSimpleModeEnabled(false)
            assertFalse(settingsRepo.simpleModeEnabled.first())
        }

        settingsRepo.setSimpleModeEnabled(true)
        assertTrue(settingsRepo.simpleModeEnabled.first())
    }

    @Test
    fun simpleMode_concurrentBackgroundWork_preservesIntegrity() = runBlocking {
        val settingsRepo = FakeSettingsRepository(simpleMode = false)
        val diffRepo = FakeDifficultySettingsRepository(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.EASY, daysRequiredPerLevel = 5),
        )
        val viewModel = DifficultyViewModel(diffRepo, settingsRepo)

        // Simulate concurrent execution: background task updates difficulty while Simple Mode switches on
        diffRepo.updateSettings(
            DifficultySettingsEntity(id = 1, difficultyLevel = DifficultyLevel.HARD, daysRequiredPerLevel = 10),
        )
        settingsRepo.setSimpleModeEnabled(true)

        val finalDiff = diffRepo.getCurrentDifficulty().first()
        val finalSimple = settingsRepo.simpleModeEnabled.first()

        assertEquals(DifficultyLevel.HARD, finalDiff?.difficultyLevel)
        assertTrue(finalSimple)
        assertTrue(viewModel.uiState.value.isSimpleModeEnabled)
    }

    @Test
    fun difficultySelection_displayMatrix_comicSkin_andFontScale2x() {
        val diffRepo = FakeDifficultySettingsRepository()
        val viewModel = DifficultyViewModel(diffRepo)

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, fontScale = 2.0f),
        ) {
            DifficultySelectionScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DIFFICULTY").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIUM").assertIsDisplayed()
    }

    @Test
    fun difficultySelection_displayMatrix_pixelSkin_andRtl() {
        val diffRepo = FakeDifficultySettingsRepository()
        val viewModel = DifficultyViewModel(diffRepo)

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            DifficultySelectionScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DIFFICULTY").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIUM").assertIsDisplayed()
    }
}
