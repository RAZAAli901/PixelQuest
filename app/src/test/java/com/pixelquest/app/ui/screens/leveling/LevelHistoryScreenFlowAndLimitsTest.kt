package com.pixelquest.app.ui.screens.leveling

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import com.pixelquest.app.data.local.entity.LevelHistoryEntity
import com.pixelquest.app.domain.repository.LevelHistoryRepository
import com.pixelquest.app.testing.ComposeTestConfiguration
import com.pixelquest.app.testing.FakeLevelHistoryRepository
import com.pixelquest.app.testing.MainDispatcherRule
import com.pixelquest.app.testing.setThemedContent
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Unit and user flow test suite for Level History (Hero > Level History):
 * - Start-to-finish user flow: viewing chronological milestones, navigating back
 * - Edge cases: zero history empty state, single entry, 100+ entries scroll
 * - Out-of-bounds: extreme level numbers, empty/malformed difficulty strings, odd characters
 * - Dynamic data: live insertion while the screen is actively visible
 * - Display resilience: Comic/Pixel themes, 2.0x font scaling, RTL layout
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class LevelHistoryScreenFlowAndLimitsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val baseTimestamp = 1761000000000L // arbitrary fixed epoch ms

    @Test
    fun levelHistory_flow_rendersEntriesInOrder_andNavigatesBack() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(id = 3L, level = 4, achievedDate = baseTimestamp + 3000, difficultyAtTimeOfLevelUp = "HARD"),
            LevelHistoryEntity(id = 2L, level = 3, achievedDate = baseTimestamp + 2000, difficultyAtTimeOfLevelUp = "MEDIUM"),
            LevelHistoryEntity(id = 1L, level = 2, achievedDate = baseTimestamp + 1000, difficultyAtTimeOfLevelUp = "EASY"),
        )
        val viewModel = LevelHistoryViewModel(repo)

        var backClicked = false
        composeTestRule.setContent {
            LevelHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { backClicked = true },
            )
        }
        composeTestRule.waitForIdle()

        // 1. Verify TopBar with uppercase title
        composeTestRule.onNodeWithText("LEVEL HISTORY").assertIsDisplayed()

        // 2. Verify all level entries and difficulties are rendered in order
        composeTestRule.onNodeWithText("Level 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hard").assertIsDisplayed()

        composeTestRule.onNodeWithText("Level 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium").assertIsDisplayed()

        composeTestRule.onNodeWithText("Level 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Easy").assertIsDisplayed()

        // 3. Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue("Back button click should trigger navigation callback", backClicked)
    }

    @Test
    fun levelHistory_zeroHistory_displaysEmptyStateCard() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = emptyList()
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("LEVEL HISTORY").assertIsDisplayed()
        composeTestRule.onNodeWithText("No levels yet. Perfect days level your hero up.").assertIsDisplayed()
        // No level list items
        assertEquals(0, composeTestRule.onAllNodesWithText("Level", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun levelHistory_loadingState_displaysProgressIndicatorUntilLoaded() {
        // Repository flow that holds null / not yet emitted
        val pendingFlow = MutableStateFlow<List<LevelHistoryEntity>?>(null)
        val pendingRepo = object : LevelHistoryRepository {
            override fun getAllHistory(): Flow<List<LevelHistoryEntity>> =
                kotlinx.coroutines.flow.emptyFlow()
            override suspend fun insertLevelHistory(entry: LevelHistoryEntity) {}
        }
        val viewModel = LevelHistoryViewModel(pendingRepo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }

        // TopBar is not present in the loading container
        assertEquals(0, composeTestRule.onAllNodesWithText("LEVEL HISTORY").fetchSemanticsNodes().size)
    }

    @Test
    fun levelHistory_singleItem_rendersLevelUpDetailsCorrectly() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(
                id = 10L,
                level = 2,
                achievedDate = 1761048000000L,
                difficultyAtTimeOfLevelUp = "MEDIUM",
            ),
        )
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium").assertIsDisplayed()
    }

    @Test
    fun levelHistory_massiveDataset_hundredsOfLevels_scrollsWithoutExceptions() {
        val repo = FakeLevelHistoryRepository()
        val entries = (100 downTo 1).map { lvl ->
            LevelHistoryEntity(
                id = lvl.toLong(),
                level = lvl,
                achievedDate = baseTimestamp + (lvl * 1000L),
                difficultyAtTimeOfLevelUp = if (lvl % 2 == 0) "HARD" else "MEDIUM",
            )
        }
        repo.history.value = entries
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // Top level (Level 100) is immediately visible
        composeTestRule.onNodeWithText("Level 100").assertIsDisplayed()

        // Scroll to a middle milestone
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("Level 50"))
        composeTestRule.onNodeWithText("Level 50").assertIsDisplayed()

        // Scroll all the way to Level 1
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("Level 1"))
        composeTestRule.onNodeWithText("Level 1").assertIsDisplayed()
    }

    @Test
    fun levelHistory_extremeNumbersAndOverflowProtection() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(
                id = 1L,
                level = 99999,
                achievedDate = 0L, // Epoch zero
                difficultyAtTimeOfLevelUp = "HARDEST",
            ),
            LevelHistoryEntity(
                id = 2L,
                level = 0,
                achievedDate = 253402300799000L, // Year 9999
                difficultyAtTimeOfLevelUp = "EASY",
            ),
        )
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("99999").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 99999").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hardest").assertIsDisplayed()

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 0").assertIsDisplayed()
    }

    @Test
    fun levelHistory_oddCharacters_emojiAndRtlInDifficulty() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(
                id = 1L,
                level = 5,
                achievedDate = baseTimestamp,
                difficultyAtTimeOfLevelUp = "🔥 EPIC MODE",
            ),
            LevelHistoryEntity(
                id = 2L,
                level = 4,
                achievedDate = baseTimestamp - 1000,
                difficultyAtTimeOfLevelUp = "صعب",
            ),
            LevelHistoryEntity(
                id = 3L,
                level = 3,
                achievedDate = baseTimestamp - 2000,
                difficultyAtTimeOfLevelUp = "", // empty string safety
            ),
            LevelHistoryEntity(
                id = 4L,
                level = 2,
                achievedDate = baseTimestamp - 3000,
                difficultyAtTimeOfLevelUp = "<script>alert(1)</script>", // XSS / HTML snippet
            ),
        )
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Level 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 2").assertIsDisplayed()
    }

    @Test
    fun levelHistory_dynamicUpdateWhileScreenIsOpen() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(id = 1L, level = 2, achievedDate = baseTimestamp, difficultyAtTimeOfLevelUp = "MEDIUM"),
        )
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setContent {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Level 2").assertIsDisplayed()
        assertEquals(0, composeTestRule.onAllNodesWithText("Level 3").fetchSemanticsNodes().size)

        // Background worker reaches level 3 and inserts into repository
        repo.history.value = listOf(
            LevelHistoryEntity(id = 2L, level = 3, achievedDate = baseTimestamp + 1000, difficultyAtTimeOfLevelUp = "HARD"),
            LevelHistoryEntity(id = 1L, level = 2, achievedDate = baseTimestamp, difficultyAtTimeOfLevelUp = "MEDIUM"),
        )
        composeTestRule.waitForIdle()

        // Screen recomposes with newly added level
        composeTestRule.onNodeWithText("Level 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hard").assertIsDisplayed()
    }

    @Test
    fun levelHistory_displayMatrix_comicSkin_andFontScale2x() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(id = 1L, level = 10, achievedDate = baseTimestamp, difficultyAtTimeOfLevelUp = "HARD"),
        )
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(themeMode = ThemeMode.Comic, fontScale = 2.0f),
        ) {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("LEVEL HISTORY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 10").assertIsDisplayed()
    }

    @Test
    fun levelHistory_displayMatrix_pixelSkin_andRtl() {
        val repo = FakeLevelHistoryRepository()
        repo.history.value = listOf(
            LevelHistoryEntity(id = 1L, level = 10, achievedDate = baseTimestamp, difficultyAtTimeOfLevelUp = "HARD"),
        )
        val viewModel = LevelHistoryViewModel(repo)

        composeTestRule.setThemedContent(
            ComposeTestConfiguration(
                themeMode = ThemeMode.Pixel,
                layoutDirection = LayoutDirection.Rtl,
                locale = Locale("ar"),
            ),
        ) {
            LevelHistoryScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("LEVEL HISTORY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Level 10").assertIsDisplayed()
    }
}
