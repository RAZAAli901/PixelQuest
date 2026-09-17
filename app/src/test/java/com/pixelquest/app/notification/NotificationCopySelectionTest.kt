package com.pixelquest.app.notification

import com.pixelquest.app.ui.prompt.TaskPromptCopyVariants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 17: Unit tests for notification and prompt copy-selection logic based on Simple Mode state.
 * Asserts that streak, XP, and RPG flavor text are omitted when Simple Mode is enabled,
 * and present when Gamified mode is enabled.
 */
class NotificationCopySelectionTest {

    @Test
    fun reminderTitle_omitsGamifiedTermsInSimpleMode() {
        val taskName = "Morning Run"

        val gamifiedTitle = NotificationHelper.getReminderTitle(taskName, isSimpleMode = false)
        val simpleTitle = NotificationHelper.getReminderTitle(taskName, isSimpleMode = true)

        assertTrue("Gamified title must contain RPG quest indicator", gamifiedTitle.contains("⚔️ Quest Time"))
        assertEquals("Simple Mode title must use clean reminder phrasing", "Time to do: Morning Run", simpleTitle)
        assertFalse("Simple Mode title must not contain sword emoji", simpleTitle.contains("⚔️"))
    }

    @Test
    fun reminderText_omitsStreakReferencesInSimpleMode() {
        val taskName = "Read Book"

        val gamifiedText = NotificationHelper.getReminderText(taskName, isSimpleMode = false)
        val simpleText = NotificationHelper.getReminderText(taskName, isSimpleMode = true)

        assertTrue("Gamified text must mention streak preservation", gamifiedText.contains("Keep your streak!"))
        assertEquals("Simple Mode text must use neutral completion phrasing", "Time to complete: Read Book", simpleText)
        assertFalse("Simple Mode text must not reference streaks", simpleText.contains("streak", ignoreCase = true))
    }

    @Test
    fun missedTaskTitle_usesNeutralToneInSimpleMode() {
        val taskName = "Drink Water"

        val gamifiedTitle = NotificationHelper.getMissedTaskTitle(taskName, isSimpleMode = false)
        val simpleTitle = NotificationHelper.getMissedTaskTitle(taskName, isSimpleMode = true)

        assertTrue("Gamified missed title must contain broken heart and Quest", gamifiedTitle.contains("💔 Quest Missed"))
        assertEquals("Simple Mode missed title must be neutral", "Task Missed: Drink Water", simpleTitle)
        assertFalse("Simple Mode missed title must not contain broken heart emoji", simpleTitle.contains("💔"))
    }

    @Test
    fun missedTaskText_omitsBrokenStreakAccusationInSimpleMode() {
        val taskName = "Evening Meditation"

        val gamifiedText = NotificationHelper.getMissedTaskText(taskName, isSimpleMode = false)
        val simpleText = NotificationHelper.getMissedTaskText(taskName, isSimpleMode = true)

        assertTrue("Gamified missed text must reference broken streak", gamifiedText.contains("broke your streak"))
        assertEquals(
            "Simple Mode missed text must be calm and neutral",
            "You missed a scheduled task: Evening Meditation.",
            simpleText
        )
        assertFalse("Simple Mode missed text must not mention broken streak", simpleText.contains("streak", ignoreCase = true))
    }

    @Test
    fun taskPromptCopyVariants_resolvesCorrectlyPerMode() {
        val gamifiedCopy = TaskPromptCopyVariants.resolve(isSimpleMode = false)
        val simpleCopy = TaskPromptCopyVariants.resolve(isSimpleMode = true)

        // Gamified copy checks
        assertEquals("⚔️", gamifiedCopy.iconEmoji)
        assertEquals("DID YOU DO IT?", gamifiedCopy.headerTitle)
        assertEquals("YES!", gamifiedCopy.confirmButtonText)
        assertEquals("NOT YET", gamifiedCopy.dismissButtonText)
        assertTrue(gamifiedCopy.showPointsBadge)

        // Simple Mode copy checks
        assertNull("Simple Mode prompt must have no RPG icon emoji", simpleCopy.iconEmoji)
        assertEquals("TASK REMINDER", simpleCopy.headerTitle)
        assertEquals("Completed", simpleCopy.confirmButtonText)
        assertEquals("Not yet", simpleCopy.dismissButtonText)
        assertFalse("Simple Mode prompt must not display points badge", simpleCopy.showPointsBadge)
    }
}
