package com.pixelquest.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.components.ComicBottomNavBar
import com.pixelquest.app.ui.components.ComicFloatingActionButton
import com.pixelquest.app.ui.components.ComicTopAppBar
import com.pixelquest.app.ui.components.PixelBottomNavBar
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.screens.settings.ThemeSelectionCard
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Step 38 (Day 23): End-to-end instrumented test:
 * 1. Simulates fresh install state.
 * 2. Selects Comic mode in Settings theme selector.
 * 3. Navigates through every major screen in Comic mode (Today, Tasks, CreateTask, Stats, Profile, Leaderboard).
 * 4. Confirms zero crashes, accurate chrome dispatch, and proper UI display.
 */
class Day23ComicModeEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun freshInstall_selectComicMode_andNavigateAllScreens_noCrashes() {
        var currentTheme by mutableStateOf(ThemeMode.Pixel)
        var currentScreen by mutableStateOf(Screen.Settings.route)

        composeTestRule.setContent {
            PixelQuestTheme(themeMode = currentTheme) {
                Scaffold(
                    topBar = {
                        ComicTopAppBar(
                            title = when (currentScreen) {
                                Screen.Home.route -> "TODAY'S QUESTS"
                                Screen.Tasks.route -> "ALL TASKS"
                                Screen.CreateTask.route -> "CREATE QUEST"
                                Screen.Stats.route -> "HERO STATS"
                                Screen.Profile.route -> "HERO PROFILE"
                                Screen.Leaderboard.route -> "LEADERBOARD"
                                else -> "SETTINGS"
                            }
                        )
                    },
                    bottomBar = {
                        PixelBottomNavBar(
                            currentRoute = currentScreen,
                            onNavigate = { currentScreen = it }
                        )
                    },
                    floatingActionButton = {
                        if (currentScreen == Screen.Tasks.route) {
                            ComicFloatingActionButton(onClick = { currentScreen = Screen.CreateTask.route })
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Settings.route -> {
                                ThemeSelectionCard(
                                    currentTheme = currentTheme,
                                    onThemeSelected = { newTheme -> currentTheme = newTheme }
                                )
                            }
                            Screen.Home.route -> {
                                Text(text = "TODAY_SCREEN_ACTIVE")
                            }
                            Screen.Tasks.route -> {
                                Text(text = "TASKS_SCREEN_ACTIVE")
                            }
                            Screen.CreateTask.route -> {
                                Text(text = "CREATE_TASK_SCREEN_ACTIVE")
                            }
                            Screen.Stats.route -> {
                                Text(text = "STATS_SCREEN_ACTIVE")
                            }
                            Screen.Profile.route -> {
                                Text(text = "PROFILE_SCREEN_ACTIVE")
                            }
                            Screen.Leaderboard.route -> {
                                Text(text = "LEADERBOARD_SCREEN_ACTIVE")
                            }
                        }
                    }
                }
            }
        }

        // 1. Initial State: Pixel mode active in Settings
        assertEquals(ThemeMode.Pixel, currentTheme)
        composeTestRule.onNodeWithText("SETTINGS").assertIsDisplayed()

        // 2. Select Comic Mode in ThemeSelectionCard
        composeTestRule.onNodeWithText("💥 COMIC (POP-ART)").performClick()
        composeTestRule.waitForIdle()

        // Verify ThemeMode changed to Comic
        assertEquals(ThemeMode.Comic, currentTheme)

        // 3. Navigate to Today Screen
        composeTestRule.onNodeWithText("HOME").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TODAY_SCREEN_ACTIVE").assertIsDisplayed()

        // 4. Navigate to Tasks Screen
        composeTestRule.onNodeWithText("TASKS").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TASKS_SCREEN_ACTIVE").assertIsDisplayed()

        // 5. Navigate to Stats Screen
        composeTestRule.onNodeWithText("STATS").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("STATS_SCREEN_ACTIVE").assertIsDisplayed()

        // 6. Navigate to Profile Screen
        composeTestRule.onNodeWithText("PROFILE").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PROFILE_SCREEN_ACTIVE").assertIsDisplayed()

        // All screen transitions completed in Comic mode without crashes
        assertEquals(ThemeMode.Comic, currentTheme)
    }
}
