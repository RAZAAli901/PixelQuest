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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithContentDescription
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

    private fun notifications(enabled: Boolean) {
        val manager = composeTestRule.activity.getSystemService(android.app.NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(enabled)
    }

    private var batteryTipDismissed by androidx.compose.runtime.mutableStateOf(false)

    private fun show() {
        composeTestRule.setContent {
            PixelQuestTheme {
                NamazReminderChecks(batteryTipDismissed = batteryTipDismissed, onDismissBatteryTip = { batteryTipDismissed = true })
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun batteryLimited(limited: Boolean) {
        val power = composeTestRule.activity.getSystemService(android.os.PowerManager::class.java)
        shadowOf(power).setIgnoringBatteryOptimizations(composeTestRule.activity.packageName, !limited)
    }

    @Test
    fun withoutExactAlarms_aCardSaysRemindersMayBeLate_andOpensTheSetting() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        show()

        composeTestRule.onNodeWithTag(NamazTestTags.EXACT_ALARM_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText("ALLOW EXACT TIMES").performClick()

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

    @Test
    fun withNotificationsOff_aCardSaysRemindersCantAppear_andAsksOnce_thenOpensTheSettings() {
        notifications(enabled = false)
        show()
        composeTestRule.onNodeWithTag(NamazTestTags.NOTIFICATIONS_CARD).assertIsDisplayed()

        // Android 13+: its own prompt first.
        composeTestRule.onNodeWithText("ALLOW NOTIFICATIONS").performClick()
        val asked = shadowOf(composeTestRule.activity).lastRequestedPermission
        assertEquals(listOf(android.Manifest.permission.POST_NOTIFICATIONS), asked.requestedPermissions.toList())

        // After that, the app's notification settings.
        composeTestRule.onNodeWithText("ALLOW NOTIFICATIONS").performClick()
        val opened = shadowOf(composeTestRule.activity).nextStartedActivity
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, opened.action)
        assertEquals(composeTestRule.activity.packageName, opened.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    fun withThePermissionGranted_butNotificationsOffInSettings_itOpensTheSettingsStraightAway() {
        shadowOf(composeTestRule.activity.application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        notifications(enabled = false)
        show()

        composeTestRule.onNodeWithText("ALLOW NOTIFICATIONS").performClick()

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, shadowOf(composeTestRule.activity).nextStartedActivity.action)
    }

    @Test
    fun withNotificationsOn_thereIsNoNotificationsCard() {
        notifications(enabled = true)
        show()

        composeTestRule.onNodeWithTag(NamazTestTags.NOTIFICATIONS_CARD).assertDoesNotExist()
    }

    @Test
    fun whileBatteryIsOptimised_aTipLinksToTheBatterySettings_andCanBeHidden() {
        batteryLimited(true)
        show()
        composeTestRule.onNodeWithTag(NamazTestTags.BATTERY_CARD).assertIsDisplayed()

        composeTestRule.onNodeWithText("OPEN BATTERY SETTINGS").performClick()
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, shadowOf(composeTestRule.activity).nextStartedActivity.action)

        composeTestRule.onNodeWithContentDescription("Hide this tip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(NamazTestTags.BATTERY_CARD).assertDoesNotExist()
    }

    @Test
    fun withoutBatteryLimits_thereIsNoTip() {
        batteryLimited(false)
        show()

        composeTestRule.onNodeWithTag(NamazTestTags.BATTERY_CARD).assertDoesNotExist()
    }
}
