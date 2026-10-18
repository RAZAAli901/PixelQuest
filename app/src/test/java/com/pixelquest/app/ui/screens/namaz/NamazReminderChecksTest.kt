package com.pixelquest.app.ui.screens.namaz

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pixelquest.app.ui.theme.PixelQuestTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/** The cards that say what the phone is doing to the reminders, and how to fix it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w411dp-h914dp")
class NamazReminderChecksTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    private fun show() {
        composeTestRule.setContent { PixelQuestTheme { NamazReminderChecks() } }
        composeTestRule.waitForIdle()
    }

    @Test
    fun withoutExactAlarms_aCardSaysRemindersMayBeLate_andOpensTheSetting() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        show()

        composeTestRule.onNodeWithTag(NamazTestTags.EXACT_ALARM_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText("Allow exact times").performClick()

        val opened = shadowOf(composeTestRule.activity).nextStartedActivity
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, opened.action)
        assertEquals("package:" + composeTestRule.activity.packageName, opened.dataString)
    }

    @Test
    fun withExactAlarms_thereIsNoCard() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        show()

        composeTestRule.onNodeWithTag(NamazTestTags.EXACT_ALARM_CARD).assertDoesNotExist()
    }
}
