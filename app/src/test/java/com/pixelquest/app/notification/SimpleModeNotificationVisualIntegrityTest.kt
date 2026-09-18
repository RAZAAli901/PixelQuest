package com.pixelquest.app.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeNotificationVisualIntegrityTest {

    @Test
    fun `notification visuals in simple mode do not contain gamified iconography or words`() {
        val taskName = "Daily Review"
        val reminderTitle = NotificationHelper.getReminderTitle(taskName, isSimpleMode = true)
        val reminderText = NotificationHelper.getReminderText(taskName, isSimpleMode = true)
        val missedTitle = NotificationHelper.getMissedTaskTitle(taskName, isSimpleMode = true)
        val missedText = NotificationHelper.getMissedTaskText(taskName, isSimpleMode = true)

        val gamifiedMarkers = listOf("⚔️", "Quest", "streak", "💔", "hero", "level", "XP")

        gamifiedMarkers.forEach { marker ->
            assertFalse("Reminder title should not contain '$marker'", reminderTitle.contains(marker, ignoreCase = true))
            assertFalse("Reminder text should not contain '$marker'", reminderText.contains(marker, ignoreCase = true))
            assertFalse("Missed title should not contain '$marker'", missedTitle.contains(marker, ignoreCase = true))
            assertFalse("Missed text should not contain '$marker'", missedText.contains(marker, ignoreCase = true))
        }

        assertTrue(reminderTitle.startsWith("Time to do:"))
        assertTrue(reminderText.startsWith("Time to complete:"))
        assertTrue(missedTitle.startsWith("Task Missed:"))
    }

    @Test
    fun `gamified mode notification copy preserves RPG themes and icons`() {
        val taskName = "Daily Review"
        val reminderTitle = NotificationHelper.getReminderTitle(taskName, isSimpleMode = false)
        val reminderText = NotificationHelper.getReminderText(taskName, isSimpleMode = false)
        val missedTitle = NotificationHelper.getMissedTaskTitle(taskName, isSimpleMode = false)
        val missedText = NotificationHelper.getMissedTaskText(taskName, isSimpleMode = false)

        assertTrue(reminderTitle.contains("⚔️"))
        assertTrue(reminderTitle.contains("Quest"))
        assertTrue(reminderText.contains("streak"))
        assertTrue(missedTitle.contains("💔"))
        assertTrue(missedTitle.contains("Quest"))
        assertTrue(missedText.contains("streak"))
    }
}
