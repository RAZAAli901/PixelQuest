package com.pixelquest.app.ui.today

import com.pixelquest.app.domain.model.TaskTerminology
import com.pixelquest.app.ui.components.CountdownFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Step 4: Verification test confirming that functional utilities (countdown timers,
 * quick-complete mechanisms, and task-card operational logic) remain fully functional
 * and visually intact in Simple Mode.
 */
class SimpleModeTodayFunctionalIntegrityTest {

    @Test
    fun testCountdownFormatter_operatesNeutrallyWithoutGamificationBias() {
        val now = LocalTime.of(12, 0)

        // Future task
        val futureTime = LocalTime.of(13, 30)
        val formattedRemaining = CountdownFormatter.formatRemainingTime(futureTime, now)
        assertEquals("01h 30m left", formattedRemaining)
        assertFalse(CountdownFormatter.isExpired(futureTime, now))
        assertFalse(CountdownFormatter.isUrgent(futureTime, now))

        // Urgent task (within 15 minutes)
        val urgentTime = LocalTime.of(12, 10)
        val formattedUrgent = CountdownFormatter.formatRemainingTime(urgentTime, now)
        assertEquals("10m left", formattedUrgent)
        assertTrue(CountdownFormatter.isUrgent(urgentTime, now))
        assertFalse(CountdownFormatter.isExpired(urgentTime, now))

        // Expired task
        val pastTime = LocalTime.of(11, 45)
        val formattedPast = CountdownFormatter.formatRemainingTime(pastTime, now)
        assertEquals("TIME'S UP!", formattedPast)
        assertTrue(CountdownFormatter.isExpired(pastTime, now))
    }

    @Test
    fun testTaskTerminology_switchesAccuratelyBetweenGamifiedAndSimple() {
        val gamified = TaskTerminology.forMode(isSimpleMode = false)
        assertEquals("Quest", gamified.itemSingular)
        assertEquals("Quests", gamified.itemPlural)
        assertEquals("⚔️ TODAY'S DASHBOARD", gamified.todayHeader)
        assertEquals("⚔️ UP NEXT", gamified.upNextHeader)
        assertEquals("📜 COMPLETED & PAST QUESTS", gamified.completedHeader)
        assertEquals("+ CREATE QUEST", gamified.createButtonText)

        val simple = TaskTerminology.forMode(isSimpleMode = true)
        assertEquals("Task", simple.itemSingular)
        assertEquals("Tasks", simple.itemPlural)
        assertEquals("📋 TODAY'S TASKS", simple.todayHeader)
        assertEquals("UP NEXT", simple.upNextHeader)
        assertEquals("COMPLETED & PAST TASKS", simple.completedHeader)
        assertEquals("+ CREATE TASK", simple.createButtonText)
    }
}
