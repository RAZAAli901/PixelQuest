package com.pixelquest.app.ui.screens.insight

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.ai.HabitInsightResponse
import com.pixelquest.app.ui.components.PixelBottomNavBar
import com.pixelquest.app.ui.components.bottomNavItems
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The compact AI Coach (docs/ai-coach-compact.md) in Pixel, Comic, Light and Simple Mode: a
 * three-line preview that expands, earlier insights folded to one line, and all of it on a 6-inch
 * phone's screen without scrolling. Robolectric renders and measures the real layout; the app has
 * no screenshot library, and the matching previews are in AiInsightScreenPreview.kt.
 */
@RunWith(RobolectricTestRunner::class)
// A 6-inch phone: 360 x 640dp is what's left between the status bar and the bottom tabs.
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h640dp")
class CompactCoachUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val summary = "You completed 18 of 21 quests this week, with mornings your strongest time: every fitness quest " +
        "before 9am got done, while two of three evening reading quests slipped."
    private val suggestion = "Move one reading quest to the morning, right after your workout, when your streak of " +
        "early wins makes the next one easier to start."
    private val encouragement = "Three perfect days in a row is your best run yet. Keep the morning rhythm going."

    private val success = AiInsightUiState.Success(
        insight = HabitInsightResponse(summary, suggestion, encouragement, generatedAt = 1_760_000_000_000L),
        isCached = true,
        remainingCooldownSeconds = 0L
    )

    private val older = listOf(
        OlderInsight(2, HabitInsightResponse("Mornings went well, evenings slipped.", "Try reading at lunch.", "Keep going.", generatedAt = 1_759_900_000_000L)),
        OlderInsight(1, HabitInsightResponse("A steady start to the week.", "Add a short walk.", "Nice work.", generatedAt = 1_759_800_000_000L))
    )

    private fun show(
        mode: ThemeMode,
        display: CoachDisplayState = CoachDisplayState(),
        simple: Boolean = false,
        onToggle: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                AiInsightScreenContent(
                    uiState = success,
                    isSimpleMode = simple,
                    displayState = display,
                    onRefresh = {},
                    onToggleLatestExpanded = onToggle,
                    olderInsights = older
                )
            }
        }
    }

    private fun expandLabel(mode: ThemeMode) = if (mode == ThemeMode.Light) "Show more ▾" else "SHOW MORE ▾"
    private fun earlierLabel(mode: ThemeMode) = if (mode == ThemeMode.Light) "Earlier insights" else "EARLIER INSIGHTS"

    private fun compactFitsWithoutScrolling(mode: ThemeMode, simple: Boolean = false) {
        show(mode, simple = simple)

        composeTestRule.onNodeWithText(summary).assertIsDisplayed()
        assertEquals("collapsed: the suggestion waits behind SHOW MORE", 0, composeTestRule.onAllNodesWithText(suggestion).fetchSemanticsNodes().size)
        // Everything down to the last earlier insight is on screen without scrolling.
        composeTestRule.onNodeWithText(expandLabel(mode)).assertIsDisplayed()
        composeTestRule.onNodeWithText(earlierLabel(mode)).assertIsDisplayed()
        composeTestRule.onNodeWithText("A steady start to the week.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Get a fresh insight").assertIsDisplayed()
    }

    @Test fun compact_pixel_fitsA6InchPhone() = compactFitsWithoutScrolling(ThemeMode.Pixel)

    @Test fun compact_comic_fitsA6InchPhone() = compactFitsWithoutScrolling(ThemeMode.Comic)

    @Test fun compact_light_fitsA6InchPhone() = compactFitsWithoutScrolling(ThemeMode.Light)

    @Test fun compact_simpleMode_fitsA6InchPhone() {
        compactFitsWithoutScrolling(ThemeMode.Pixel, simple = true)
        // The top bar and the header both say it.
        assertTrue(composeTestRule.onAllNodesWithText("HABIT COACH").fetchSemanticsNodes().isNotEmpty())
        assertEquals("no quest words in Simple Mode", 0, composeTestRule.onAllNodesWithText("QUESTMASTER COACH").fetchSemanticsNodes().size)
    }

    private fun expandedShowsEverything(mode: ThemeMode) {
        show(mode, display = CoachDisplayState(latestExpanded = true))

        composeTestRule.onNodeWithText(summary).assertExists()
        composeTestRule.onNodeWithText(suggestion).assertExists()
        composeTestRule.onNodeWithText(encouragement).assertExists()
        composeTestRule.onNodeWithText(if (mode == ThemeMode.Light) "Show less ▴" else "SHOW LESS ▴").assertExists()
    }

    @Test fun expanded_pixel_showsAllThreeSections() = expandedShowsEverything(ThemeMode.Pixel)

    @Test fun expanded_comic_showsAllThreeSections() = expandedShowsEverything(ThemeMode.Comic)

    @Test fun expanded_light_showsAllThreeSections() = expandedShowsEverything(ThemeMode.Light)

    @Test
    fun notCompact_showsEverything_withoutAToggle() {
        show(ThemeMode.Pixel, display = CoachDisplayState(compact = false))

        composeTestRule.onNodeWithText(suggestion).assertExists()
        assertEquals(0, composeTestRule.onAllNodesWithText("SHOW MORE ▾").fetchSemanticsNodes().size)
        assertEquals(0, composeTestRule.onAllNodesWithText("SHOW LESS ▴").fetchSemanticsNodes().size)
    }

    @Test
    fun theToggle_asksToExpand() {
        var toggled = 0
        show(ThemeMode.Comic, onToggle = { toggled++ })

        composeTestRule.onNodeWithText("SHOW MORE ▾").performClick()

        assertEquals(1, toggled)
    }

    @Test
    fun anEarlierInsight_opens_toShowItsSuggestion() {
        show(ThemeMode.Light, display = CoachDisplayState(expandedOlderId = 2))

        composeTestRule.onNodeWithText("Try reading at lunch.").assertExists()
        assertEquals("the other stays one line", 0, composeTestRule.onAllNodesWithText("Add a short walk.").fetchSemanticsNodes().size)
    }

    private fun barIsEven(mode: ThemeMode) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = mode) {
                PixelBottomNavBar(currentRoute = Screen.Home.route, onNavigate = {})
            }
        }
        val widths = bottomNavItems.map { item ->
            val bounds = composeTestRule.onNodeWithContentDescription(item.title).getBoundsInRoot()
            assertTrue("${item.title} is a full-height tab", bounds.bottom - bounds.top >= 48.dp)
            bounds.right - bounds.left
        }
        val narrowest = widths.minOf { it.value }
        val widest = widths.maxOf { it.value }
        // The Leaderboard used to be a 44-48dp round button among wider tabs.
        assertTrue("tabs $widths", widest - narrowest <= 1f)
    }

    @Test fun pixelBar_tabsAreEven() = barIsEven(ThemeMode.Pixel)

    @Test fun lightBar_tabsAreEven() = barIsEven(ThemeMode.Light)

    @Test fun comicBar_tabsAreEven() = barIsEven(ThemeMode.Comic)
}
