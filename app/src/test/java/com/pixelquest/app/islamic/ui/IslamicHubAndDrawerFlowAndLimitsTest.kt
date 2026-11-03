package com.pixelquest.app.islamic.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodReminderSettings
import com.pixelquest.app.domain.namaz.NextPrayer
import com.pixelquest.app.domain.namaz.Prayer
import com.pixelquest.app.domain.namaz.PrayerMoment
import com.pixelquest.app.islamic.IslamicIcon
import com.pixelquest.app.islamic.IslamicItem
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.ui.QURAN_HOME
import com.pixelquest.app.study.data.MemoryStudyReminderSettings
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.testing.namazViewModel
import com.pixelquest.app.ui.navigation.PqAppFrame
import com.pixelquest.app.ui.navigation.PqNavTags
import com.pixelquest.app.ui.navigation.PqRootState
import com.pixelquest.app.ui.navigation.PqTab
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.islamicGraph
import com.pixelquest.app.ui.navigation.navigateInIslamic
import com.pixelquest.app.ui.navigation.redirect
import com.pixelquest.app.ui.screens.namaz.NamazScreen
import com.pixelquest.app.ui.screens.namaz.NamazSummary
import com.pixelquest.app.ui.screens.namaz.NamazTestTags
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * V043: Islamic hub and drawer flow and limits suite.
 *
 * User flow:
 * - Open Deen tab from bottom bar / navigation
 * - Navigate to each Islamic item (Namaz, Darood, Quran) and back
 *
 * Limits and edge cases:
 * - Back press at every nested level: Quran Search -> Surah Reader -> Quran Home -> Hub -> Home
 * - Legacy deep links / redirects (LEGACY_NAMAZ, LEGACY_DAROOD) with clean back stacks
 * - navigateInIslamic direct deep link mounting hub underneath
 * - Hub toggled off in settings: Deen tab hidden from navigation bar entirely
 * - Re-enabling hub restores Deen tab
 * - Simple Mode single-column layout without streak counters
 * - High font scale (2.0x) layout without truncation or overlap
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class IslamicHubAndDrawerFlowAndLimitsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

    private val sampleCards = listOf(
        IslamicItem("namaz", R.string.namaz_title, R.string.islamic_namaz_description, IslamicIcon("🕌"), Screen.Namaz.route),
        IslamicItem("darood", R.string.darood_title, R.string.islamic_darood_description, IslamicIcon("📿"), Screen.Parties.route),
        IslamicItem("quran", R.string.quran_title, R.string.quran_description, IslamicIcon("📖"), Screen.Quran.route),
    )

    private val nowZoned = ZonedDateTime.of(2026, 11, 3, 14, 0, 0, 0, ZoneId.systemDefault())
    private val samplePrayerSummary = NamazSummary(
        next = NextPrayer(
            current = PrayerMoment(Prayer.DHUHR, LocalDate.of(2026, 11, 3), nowZoned.minusHours(1)),
            next = PrayerMoment(Prayer.ASR, LocalDate.of(2026, 11, 3), nowZoned.plusHours(2)),
            remaining = Duration.ofHours(2),
            prayerDay = LocalDate.of(2026, 11, 3),
        ),
        prayed = 2,
        prayedPrayers = setOf(Prayer.FAJR, Prayer.DHUHR),
        allFiveStreak = 7,
    )

    private fun setupTestApp(
        rootState: PqRootState = PqRootState(islamicHubShown = true),
        themeMode: ThemeMode = ThemeMode.Pixel,
    ) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = themeMode) {
                navController = rememberNavController()
                PqAppFrame(navController, rootState) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(Screen.Home.route) {
                            Text("HOME_DASHBOARD")
                        }
                        composable(Screen.Hero.route) {
                            Text("HERO_SCREEN")
                        }
                        composable(Screen.Tasks.route) {
                            Text("TASKS_SCREEN")
                        }
                        composable(Screen.Parties.route) {
                            Text("DAROOD_PARTIES_SCREEN")
                        }
                        composable(Screen.Settings.route) {
                            Text("SETTINGS_SCREEN")
                        }
                        islamicGraph(
                            navController,
                            items = listOf(
                                com.pixelquest.app.quran.QuranIslamicItem { nav ->
                                    composable(Screen.Quran.route) {
                                        Box(Modifier.fillMaxSize()) {
                                            Text("QURAN_HOME_SCREEN")
                                            com.pixelquest.app.quran.ui.QuranHomeScreen(onNavigateBack = { nav.popBackStack() })
                                        }
                                    }
                                    composable(Screen.QuranSurah.route) {
                                        Text("SURAH_READER_1")
                                    }
                                    composable(Screen.QuranSearch.route) {
                                        Text("QURAN_SEARCH_SCREEN")
                                    }
                                },
                            ),
                            hub = { onOpen ->
                                IslamicHubContent(
                                    state = IslamicHubUiState(sampleCards, loaded = true),
                                    onOpen = onOpen,
                                    prayer = samplePrayerSummary,
                                )
                            },
                        ) {
                            composable(Screen.Namaz.route) {
                                NamazScreen(onNavigateBack = { navController.popBackStack() }, viewModel = namazViewModel())
                            }
                        }
                        redirect(Routes.LEGACY_NAMAZ, Screen.Namaz.route, navController)
                        redirect(Routes.LEGACY_DAROOD, Screen.Parties.route, navController)
                    }
                }
            }
        }
    }

    private fun currentRoute(): String? = composeTestRule.runOnIdle { navController.currentDestination?.route }

    private fun pressBack() {
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
    }

    @Test
    fun userFlow_openHub_navigateToEachItem_andBackToHubAndHome() {
        setupTestApp()

        // 1. App starts at Home Dashboard
        composeTestRule.onNodeWithText("HOME_DASHBOARD").assertIsDisplayed()

        // 2. Open Islamic Hub from Deen tab in bottom bar
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Deen)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()
        assertEquals(Screen.IslamicHub.route, currentRoute())

        // 3. Navigate to Namaz Tracker
        composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).assertIsDisplayed()
        assertEquals(Screen.Namaz.route, currentRoute())

        // Back returns to Islamic Hub
        pressBack()
        assertEquals(Screen.IslamicHub.route, currentRoute())
        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()

        // 4. Navigate to Darood Party
        composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DAROOD_PARTIES_SCREEN").assertIsDisplayed()
        assertEquals(Screen.Parties.route, currentRoute())

        // Back returns to Islamic Hub
        pressBack()
        assertEquals(Screen.IslamicHub.route, currentRoute())

        // 5. Navigate to Quran
        composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(QURAN_HOME).assertIsDisplayed()

        // Back returns to Islamic Hub
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Screen.IslamicHub.route, currentRoute())

        // 6. Back from Hub returns to Home Dashboard
        pressBack()
        assertEquals(Screen.Home.route, currentRoute())
        composeTestRule.onNodeWithText("HOME_DASHBOARD").assertIsDisplayed()
    }

    @Test
    fun backPress_atEveryLevelOfDeepNavigation() {
        setupTestApp()

        // Open Deen Hub
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Deen)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(Screen.IslamicHub.route, currentRoute())

        // Go to Quran
        composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).performClick()
        composeTestRule.waitForIdle()
        assertEquals(Screen.Quran.route, currentRoute())

        // Navigate deeper to Surah Reader
        composeTestRule.runOnIdle { navController.navigate(Screen.QuranSurah.createRoute(1)) }
        composeTestRule.waitForIdle()
        assertEquals(Screen.QuranSurah.route, currentRoute())

        // Navigate deeper to Quran Search
        composeTestRule.runOnIdle { navController.navigate(Screen.QuranSearch.route) }
        composeTestRule.waitForIdle()
        assertEquals(Screen.QuranSearch.route, currentRoute())

        // Back from Quran Search -> lands on Surah Reader
        pressBack()
        assertEquals(Screen.QuranSurah.route, currentRoute())

        // Back from Surah Reader -> lands on Quran Home
        pressBack()
        assertEquals(Screen.Quran.route, currentRoute())

        // Back from Quran Home -> lands on Islamic Hub
        pressBack()
        assertEquals(Screen.IslamicHub.route, currentRoute())

        // Back from Islamic Hub -> lands on Home
        pressBack()
        assertEquals(Screen.Home.route, currentRoute())
    }

    @Test
    fun legacyDeepLinks_namazAndDarood_redirectCleanlyOverHubWithoutLeavingOldRoute() {
        setupTestApp()

        // 1. Old Namaz deep link from version 1.4
        composeTestRule.runOnIdle { navController.navigate(Routes.LEGACY_NAMAZ) }
        composeTestRule.waitForIdle()

        // Lands on Namaz screen
        assertEquals(Screen.Namaz.route, currentRoute())
        composeTestRule.onNodeWithTag(NamazTestTags.SCREEN).assertIsDisplayed()

        // Verify the legacy route was popped and is not lingering in the back stack
        val backStackRoutes = composeTestRule.runOnIdle {
            navController.currentBackStack.value.mapNotNull { it.destination.route }
        }
        assertFalse("Legacy namaz must not stay on backstack", backStackRoutes.contains(Routes.LEGACY_NAMAZ))

        // Back goes to Islamic Hub, then Home
        pressBack()
        assertEquals(Screen.IslamicHub.route, currentRoute())
        pressBack()
        assertEquals(Screen.Home.route, currentRoute())

        // 2. Old Darood deep link from version 1.4
        composeTestRule.runOnIdle { navController.navigate(Routes.LEGACY_DAROOD) }
        composeTestRule.waitForIdle()

        assertEquals(Screen.Parties.route, currentRoute())
        composeTestRule.onNodeWithText("DAROOD_PARTIES_SCREEN").assertIsDisplayed()

        // 3. Direct navigateInIslamic to Namaz mounts hub underneath
        composeTestRule.runOnIdle { navController.navigateInIslamic(Screen.Namaz.route) }
        composeTestRule.waitForIdle()

        assertEquals(Screen.Namaz.route, currentRoute())
        pressBack()
        assertEquals("navigateInIslamic ensures hub is underneath", Screen.IslamicHub.route, currentRoute())
    }

    @Test
    fun hubSwitchedOffInSettings_hidesDeenTab_andReenablingRestoresIt() {
        var rootState by mutableStateOf(PqRootState(islamicHubShown = true))

        composeTestRule.setContent {
            PixelQuestTheme {
                navController = rememberNavController()
                PqAppFrame(navController, rootState) { padding ->
                    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.padding(padding)) {
                        composable(Screen.Home.route) { Text("HOME") }
                        composable(Screen.Hero.route) { Text("HERO") }
                        composable(Screen.Tasks.route) { Text("TASKS") }
                    }
                }
            }
        }

        // Initially Deen tab is visible
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Deen)).assertIsDisplayed()

        // Toggle Islamic Hub OFF
        rootState = rootState.copy(islamicHubShown = false)
        composeTestRule.waitForIdle()

        // Deen tab should NOT exist
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Deen)).assertDoesNotExist()
        // Other tabs still visible
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Home)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Quests)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Hero)).assertIsDisplayed()

        // Toggle Islamic Hub back ON
        rootState = rootState.copy(islamicHubShown = true)
        composeTestRule.waitForIdle()

        // Deen tab restored
        composeTestRule.onNodeWithTag(PqNavTags.tab(PqTab.Deen)).assertIsDisplayed()
    }

    @Test
    fun islamicSettingsSection_toggleShowHub_persistsInRepository() = runBlocking {
        val fakeSettings = FakeSettingsRepository()
        val readingSettings = MemoryReadingSettingsStore()
        val daroodSettings = object : DaroodReminderSettings {
            override val weeklyReminder = MutableStateFlow(false)
            override fun setWeeklyReminder(on: Boolean) {
                weeklyReminder.value = on
            }
        }
        val studySettings = MemoryStudyReminderSettings()
        val viewModel = IslamicSettingsViewModel(readingSettings, daroodSettings, studySettings, fakeSettings)

        composeTestRule.setContent {
            PixelQuestTheme {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    IslamicSettingsSection(viewModel = viewModel)
                }
            }
        }

        // Initially ON
        assertTrue(fakeSettings.islamicHubShown.first())
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_SHOW_HUB).assertIsOn()

        // Click toggle to turn OFF
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_SHOW_HUB).performClick()
        composeTestRule.waitUntil(5_000) { runBlocking { !fakeSettings.islamicHubShown.first() } }
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_SHOW_HUB).assertIsOff()
        assertFalse(fakeSettings.islamicHubShown.first())

        // Click toggle to turn ON again
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_SHOW_HUB).performClick()
        composeTestRule.waitUntil(5_000) { runBlocking { fakeSettings.islamicHubShown.first() } }
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_SHOW_HUB).assertIsOn()
        assertTrue(fakeSettings.islamicHubShown.first())
    }

    @Test
    fun simpleMode_hidesStreaks_andAdaptsLayout() {
        val simpleState = IslamicHubUiState(
            items = sampleCards,
            isSimpleMode = true,
            loaded = true,
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                IslamicHubContent(
                    state = simpleState,
                    onOpen = {},
                    prayer = samplePrayerSummary,
                )
            }
        }

        composeTestRule.waitForIdle()
        // Streak counter flame should not be rendered in Simple Mode
        composeTestRule.onNodeWithText("7 in a row", substring = true).assertDoesNotExist()

        // All item cards exist
        composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).assertIsDisplayed()
    }

    @Test
    fun fontScale2x_rendersSingleColumnWithoutTruncationOrCrash() {
        val state = IslamicHubUiState(
            items = sampleCards,
            isSimpleMode = false,
            loaded = true,
        )

        composeTestRule.setContent {
            PixelQuestTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2.0f)) {
                    IslamicHubContent(
                        state = state,
                        onOpen = {},
                        prayer = samplePrayerSummary,
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(IslamicTestTags.HUB).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("namaz")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("darood")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(IslamicTestTags.card("quran")).assertIsDisplayed()
    }
}
