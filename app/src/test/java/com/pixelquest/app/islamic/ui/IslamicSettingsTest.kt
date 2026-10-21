package com.pixelquest.app.islamic.ui

import android.app.Application
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.verticalScroll
import com.pixelquest.app.darood.data.DaroodReminderSettings
import com.pixelquest.app.quran.data.MemoryReadingSettingsStore
import com.pixelquest.app.quran.ui.reader.DisplayMode
import com.pixelquest.app.study.data.MemoryStudyReminderSettings
import com.pixelquest.app.testing.TestViewModels
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings' Islamic section: the Quran's reading defaults, saved where the reader reads them, and the reminders. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class IslamicSettingsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val reading = MemoryReadingSettingsStore()
    private val darood = object : DaroodReminderSettings {
        override val weeklyReminder = MutableStateFlow(false)
        override fun setWeeklyReminder(on: Boolean) {
            weeklyReminder.value = on
        }
    }
    private val study = MemoryStudyReminderSettings()
    private val viewModels = TestViewModels()

    @After
    fun tearDown() = viewModels.clear()

    @Test
    fun theReadingDefaults_andEachReminder_areChangedHere() {
        val vm = viewModels.keep(IslamicSettingsViewModel::class.java) { IslamicSettingsViewModel(reading, darood, study) }
        // In a scrolling column, as the Settings screen holds it.
        composeTestRule.setContent {
            PixelQuestTheme {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) { IslamicSettingsSection(viewModel = vm) }
            }
        }

        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS).assertExists()
        composeTestRule.onNodeWithText("Prayer reminders are set in the Namaz Tracker, one by one.").assertExists()

        // The reader's own setting: shown as it opens, Arabic only.
        composeTestRule.onNodeWithTag(com.pixelquest.app.quran.ui.reader.ReaderTags.mode(DisplayMode.ARABIC)).performClick()
        composeTestRule.waitUntil(5_000) { runBlocking { reading.settings.first() }.displayMode == DisplayMode.ARABIC }

        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_DAROOD_WEEKLY).performScrollTo().assertIsOff().performClick()
        assertEquals(true, darood.weeklyReminder.value)
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_STUDY_DUE).performScrollTo().assertIsOn().performClick()
        assertEquals(false, study.dueReminders.value)
        composeTestRule.onNodeWithTag(IslamicTestTags.SETTINGS_STUDY_WEEKLY).performScrollTo().performClick()
        assertEquals(true, study.weeklySummary.value)
    }
}
