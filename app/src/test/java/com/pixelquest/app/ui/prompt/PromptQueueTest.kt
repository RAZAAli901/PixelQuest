package com.pixelquest.app.ui.prompt

import android.app.Application
import android.content.Intent
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Two quests due in the same minute: both prompts are shown, one after the other. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PromptQueueTest {

    private fun launch(id: Long, name: String) = Intent()
        .putExtra(PromptQueue.EXTRA_TASK_ID, id)
        .putExtra(PromptQueue.EXTRA_TASK_NAME, name)

    @Test
    fun aSecondPrompt_waitsForTheFirst_thenShows() {
        var queue = PromptQueue() + PromptQueue.requestFrom(launch(1, "Run"))
        queue += PromptQueue.requestFrom(launch(2, "Read")) // arrives while "Run" is open

        assertEquals(PromptRequest(1, "Run"), queue.current)
        queue = queue.advance()
        assertEquals(PromptRequest(2, "Read"), queue.current)
        queue = queue.advance()
        assertNull("Then the activity closes", queue.current)
    }

    @Test
    fun theSameQuestTwice_isQueuedOnce() {
        val queue = PromptQueue() + PromptQueue.requestFrom(launch(1, "Run")) + PromptQueue.requestFrom(launch(1, "Run"))
        assertEquals(1, queue.items.size)
    }

    @Test
    fun aLaunchWithoutAQuest_isIgnored() {
        assertNull(PromptQueue.requestFrom(Intent()))
        assertNull(PromptQueue.requestFrom(null))
        assertEquals(0, (PromptQueue() + PromptQueue.requestFrom(Intent())).items.size)
    }

    @Test
    fun aMissingName_fallsBackToTask() {
        assertEquals("Task", PromptQueue.requestFrom(Intent().putExtra(PromptQueue.EXTRA_TASK_ID, 4L))!!.taskName)
    }

    @Test
    fun theQueue_survivesARotation() {
        val queue = PromptQueue(listOf(PromptRequest(1, "Run"), PromptRequest(2, "Read")))
        val state = Bundle().also { queue.saveTo(it) }

        assertEquals(queue, PromptQueue.restoreFrom(state))
        assertNull(PromptQueue.restoreFrom(null))
        assertNull(PromptQueue.restoreFrom(Bundle()))
    }

    @Test
    fun theReminderDay_travelsWithThePrompt_andSurvivesARotation() {
        val day = java.time.LocalDate.of(2026, 10, 7)
        val request = PromptQueue.requestFrom(
            launch(1, "Run").putExtra(com.pixelquest.app.scheduling.TaskAlarmScheduler.EXTRA_OCCURRENCE_DATE, day.toEpochDay())
        )!!
        assertEquals(day, request.occurrenceDate)

        val queue = PromptQueue(listOf(request, PromptRequest(2, "Read")))
        val state = Bundle().also { queue.saveTo(it) }
        assertEquals(queue, PromptQueue.restoreFrom(state))
    }
}
