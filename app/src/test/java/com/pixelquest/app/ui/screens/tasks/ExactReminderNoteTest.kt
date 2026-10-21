package com.pixelquest.app.ui.screens.tasks

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The quest form says when reminders may ring late. Seen on the emulator: Android 14 doesn't let a
 * new install set exact alarms, a reminder came 10 minutes late, and the note about it was only in
 * Settings, not where the reminder is set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h914dp")
class ExactReminderNoteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun form(reminderOn: Boolean, exactAllowed: Boolean, onAllow: () -> Unit = {}) {
        composeTestRule.setContent {
            PixelQuestTheme(themeMode = ThemeMode.Pixel) {
                TaskFormContent(
                    formState = TaskFormState(name = "Ring_Test", reminderEnabled = reminderOn),
                    canScheduleExact = { exactAllowed },
                    onAllowExact = onAllow
                )
            }
        }
    }

    private fun noteCount() = composeTestRule.onAllNodesWithText("up to 10 minutes late", substring = true).fetchSemanticsNodes().size

    @Test
    fun reminderOn_withoutExactAlarms_saysSo_andOffersToAllow() {
        var asked = 0
        form(reminderOn = true, exactAllowed = false, onAllow = { asked++ })

        assertEquals(1, noteCount())
        composeTestRule.onNodeWithText("ALLOW EXACT REMINDER TIMES").performScrollTo().performClick()
        assertEquals(1, asked)
    }

    @Test
    fun exactAlarmsAllowed_noNote() {
        form(reminderOn = true, exactAllowed = true)
        assertEquals(0, noteCount())
    }

    @Test
    fun reminderOff_noNote() {
        form(reminderOn = false, exactAllowed = false)
        assertEquals(0, noteCount())
    }
}
