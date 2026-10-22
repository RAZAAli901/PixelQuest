package com.pixelquest.app.util

import android.app.Application
import android.content.Intent
import com.pixelquest.app.MainActivity
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.ui.prompt.PromptQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** S045: every extra we read is checked against the shape we put there. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class IntentExtrasTest {

    private val id = "0b7c6f8e-1f2a-4c3d-9e8f-123456789abc"

    @Test
    fun rowIds_arePositive() {
        assertEquals(5L, IntentExtras.rowId(Intent().putExtra("k", 5L), "k"))
        assertNull(IntentExtras.rowId(Intent().putExtra("k", 0L), "k"))
        assertNull(IntentExtras.rowId(Intent().putExtra("k", -9L), "k"))
        assertNull(IntentExtras.rowId(Intent().putExtra("k", "5"), "k"))
        assertNull(IntentExtras.rowId(null, "k"))
    }

    @Test
    fun ids_mustBeUuids() {
        assertEquals(id, IntentExtras.uuid(Intent().putExtra("k", id), "k"))
        listOf("", "r1", "$id/../x", "$id\n", "' or 1=1 --").forEach {
            assertNull(it, IntentExtras.uuid(Intent().putExtra("k", it), "k"))
        }
    }

    @Test
    fun dates_stayInRange_andNeverThrow() {
        val today = LocalDate.of(2026, 10, 22)
        assertEquals(today, IntentExtras.epochDay(Intent().putExtra("k", today.toEpochDay()), "k"))
        assertNull(IntentExtras.epochDay(Intent(), "k"))
        assertNull(IntentExtras.epochDay(Intent().putExtra("k", Long.MAX_VALUE), "k"))
        assertNull(IntentExtras.epochDay(Intent().putExtra("k", -1L), "k"))
        assertNull(TaskAlarmScheduler.occurrenceDateFrom(Intent().putExtra(TaskAlarmScheduler.EXTRA_OCCURRENCE_DATE, Long.MIN_VALUE)))
    }

    @Test
    fun labels_areCleanedAndCut() {
        assertEquals("Run", IntentExtras.label(Intent().putExtra("k", " Run\u0000\u001b "), "k", "Task"))
        assertEquals("Task", IntentExtras.label(Intent().putExtra("k", "\n\t"), "k", "Task"))
        assertEquals("Task", IntentExtras.label(Intent(), "k", "Task"))
        assertEquals(IntentExtras.MAX_LABEL, IntentExtras.label(Intent().putExtra("k", "x".repeat(5000)), "k", "Task").length)
        assertEquals("Read Surah Kahf ✨", IntentExtras.label(Intent().putExtra("k", "Read Surah Kahf ✨"), "k", "Task"))
    }

    @Test
    fun thePrompt_refusesBadQuests() {
        assertNull(PromptQueue.requestFrom(Intent().putExtra(PromptQueue.EXTRA_TASK_ID, 0L)))
        val long = PromptQueue.requestFrom(Intent().putExtra(PromptQueue.EXTRA_TASK_ID, 3L).putExtra(PromptQueue.EXTRA_TASK_NAME, "y".repeat(10_000)))!!
        assertEquals(IntentExtras.MAX_LABEL, long.taskName.length)
    }

    @Test
    fun notifications_openOnlyKnownScreens() {
        listOf("settings", "chess/game/$id/../x", "chess/game/1", "account", "").forEach {
            assertFalse(it, MainActivity.isOpenable(it))
        }
        assertTrue(MainActivity.isOpenable("chess/game/$id"))
    }
}
